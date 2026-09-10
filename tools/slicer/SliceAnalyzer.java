import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 业务切片分析器。
 *
 * <p>回答存量系统接入插件框架时最难的那个问题：<b>把哪一块业务切出去做插件</b>。
 *
 * <p>做法是基于源码构建<b>包级依赖图</b>，用软件工程中成熟的稳定度指标排序，
 * 而不是靠人拍脑袋。核心指标：
 * <ul>
 *   <li>{@code fanIn}：多少个其他包依赖了它 —— 越高说明它是共享层，越不该切。</li>
 *   <li>{@code fanOut}：它依赖了多少个其他包 —— 越高说明越不独立。</li>
 *   <li>{@code stability}：{@code fanOut / (fanIn + fanOut)}，趋向 0 表示被依赖多于依赖他人。</li>
 * </ul>
 * 一个理想的切片是<b>低 fanIn、低 fanOut、有一定规模</b>的功能包。
 *
 * <p>约束：零第三方依赖，只用 JDK 内置 API，可单文件源码运行。
 * 采用源码文本解析而非字节码分析，换取"无需编译即可运行"的体验，
 * 代价是无法识别同包内的隐式引用，详见文档中的精度说明。
 *
 * <pre>
 *   java tools/slicer/SliceAnalyzer.java /path/to/your/project
 *   java tools/slicer/SliceAnalyzer.java /path/to/your/project --json --top 10
 * </pre>
 */
public class SliceAnalyzer {

    private static final Set<String> SKIP_DIRS = Set.of(
            "target", "build", "out", "node_modules", ".git", ".idea", ".mvn", ".gradle");

    /** 小于该规模的包不具备切片价值。 */
    private static final int MIN_CLASS_COUNT = 3;

    /** 自动推断包深度时，某一段包名需要获得的最低类占比。 */
    private static final double MAJORITY = 0.8;

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("(?m)^[ \\t]*package[ \\t]+([\\w.]+)[ \\t]*;");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^[ \\t]*import[ \\t]+(?:static[ \\t]+)?([\\w.]+|\\w+\\.\\*)[ \\t]*;");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern MODULE_PATTERN = Pattern.compile("<module>([^<]+)</module>");
    private static final Pattern FIELD_TYPE_PATTERN =
            Pattern.compile("(?:private|protected|public)?[ \\t]*(?:final[ \\t]+)?(?:static[ \\t]+)?"
                    + "([A-Z][\\w.]*)(?:<[^<>]*>)?[ \\t]+(\\w+)[ \\t]*(?:=|;)");

    private static final Set<String> HTTP_ANNOTATIONS =
            Set.of("RestController", "Controller", "RequestMapping", "GetMapping", "PostMapping");
    private static final Set<String> JPA_ANNOTATIONS =
            Set.of("Entity", "Table", "Column", "ManyToOne", "OneToMany", "ManyToMany");
    private static final Set<String> DAO_ANNOTATIONS = Set.of("Mapper", "Repository", "Dao");
    private static final Set<String> CONFIG_ANNOTATIONS =
            Set.of("Configuration", "SpringBootApplication", "EnableAutoConfiguration");

    public static void main(String[] args) {
        Path projectRoot = null;
        boolean json = false;
        int top = 8;
        Integer depth = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--json".equals(arg)) {
                json = true;
            } else if ("--top".equals(arg) && i + 1 < args.length) {
                top = Integer.parseInt(args[++i]);
            } else if ("--depth".equals(arg) && i + 1 < args.length) {
                depth = Integer.parseInt(args[++i]);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsage();
                return;
            } else if (arg.startsWith("-")) {
                System.err.println("未知参数: " + arg);
                printUsage();
                System.exit(2);
            } else if (projectRoot == null) {
                projectRoot = Paths.get(arg);
            }
        }

        if (projectRoot == null) {
            projectRoot = Paths.get("").toAbsolutePath();
        }
        projectRoot = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(projectRoot)) {
            System.err.println("目录不存在: " + projectRoot);
            System.exit(2);
        }

        List<ClassInfo> classes;
        try {
            classes = scanClasses(projectRoot);
        } catch (IOException e) {
            System.err.println("源码扫描失败: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (classes.isEmpty()) {
            System.out.println();
            System.out.println("未在 " + projectRoot + " 下扫描到 Java 源码。");
            System.out.println("请确认路径正确，或项目确实包含 src/main/java 目录。");
            System.out.println();
            return;
        }

        int resolvedDepth = depth != null ? depth : autoDepth(classes);
        Analysis analysis = analyze(classes, resolvedDepth, top);

        if (json) {
            System.out.println(toJson(projectRoot, analysis, resolvedDepth));
        } else {
            printReport(projectRoot, analysis, resolvedDepth, top);
        }
    }

    private static void printUsage() {
        System.out.println("用法: java SliceAnalyzer.java <项目路径> [--json] [--top N] [--depth N]");
        System.out.println();
        System.out.println("  <项目路径>  要分析的项目根目录，缺省为当前目录");
        System.out.println("  --json      以 JSON 输出");
        System.out.println("  --top N     展示前 N 个候选切片（默认 8）");
        System.out.println("  --depth N   按包名前 N 段聚合（默认自动推断）");
        System.out.println();
        System.out.println("说明: 本工具只做静态分析，不会编译或修改目标项目。");
    }

    // ---------------------------------------------------------------- 数据模型

    private static final class ClassInfo {
        final String fqcn;
        final String packageName;
        final String simpleName;
        final List<String> imports = new ArrayList<>();
        final Set<String> annotations = new LinkedHashSet<>();

        ClassInfo(String fqcn, String packageName, String simpleName) {
            this.fqcn = fqcn;
            this.packageName = packageName;
            this.simpleName = simpleName;
        }
    }

    private static final class PackageNode {
        final String name;
        final List<ClassInfo> classes = new ArrayList<>();

        PackageNode(String name) {
            this.name = name;
        }
        /** 依赖了哪些其他包。 */
        final Set<String> fanOut = new HashSet<>();
        /** 被哪些其他包依赖。 */
        final Set<String> fanIn = new HashSet<>();
        /** 依赖的外部库分组（取前两段包名）。 */
        final Set<String> externalGroups = new HashSet<>();
        /** 被其他包引用的类 -> 引用它的包集合。用"被几个包引用"而非"被引用几次"衡量契约面。 */
        final Map<String, Set<String>> contractClasses = new TreeMap<>();
        boolean hasHttp;
        boolean hasJpa;
        boolean hasDao;
        boolean hasConfig;
        boolean inCycle;
        /** 与哪些包构成双向依赖。解环时这才是真正要动的那几处。 */
        final Set<String> cyclePartners = new LinkedHashSet<>();
        double score;
    }

    private static final class Analysis {
        final List<PackageNode> candidates = new ArrayList<>();
        final List<PackageNode> shared = new ArrayList<>();
        List<PackageNode> all = new ArrayList<>();
        int classCount;
        int edgeCount;
    }

    // ---------------------------------------------------------------- 扫描

    private static List<ClassInfo> scanClasses(Path root) throws IOException {
        List<ClassInfo> result = new ArrayList<>();
        List<Path> sourceRoots = new ArrayList<>();
        for (Path scanBase : resolveScanBases(root)) {
            collectSourceRoots(scanBase, sourceRoots);
        }

        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isRegularFile(p) || !p.getFileName().toString().endsWith(".java")) {
                        continue;
                    }
                    if (Files.size(p) > 1024 * 1024L) {
                        continue;
                    }
                    ClassInfo info = inspect(p);
                    if (info != null) {
                        result.add(info);
                    }
                }
            }
        }
        result.sort(Comparator.comparing(c -> c.fqcn));
        return result;
    }

    private static ClassInfo inspect(Path file) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        String fileName = file.getFileName().toString();
        String simpleName = fileName.substring(0, fileName.lastIndexOf('.'));

        String packageName = "";
        Matcher pkg = PACKAGE_PATTERN.matcher(content);
        if (pkg.find()) {
            packageName = pkg.group(1);
        }
        String fqcn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        ClassInfo info = new ClassInfo(fqcn, packageName, simpleName);

        Matcher imp = IMPORT_PATTERN.matcher(content);
        while (imp.find()) {
            info.imports.add(imp.group(1));
        }
        Matcher ann = ANNOTATION_PATTERN.matcher(content);
        while (ann.find()) {
            info.annotations.add(ann.group(1));
        }
        return info;
    }

    /**
     * 确定扫描起点。
     *
     * <p>多模块工程里根目录常常还挂着示例工程、脚手架、文档站点，直接全量扫描会把它们
     * 当成业务代码一起分析，污染包依赖图。这里与 preflight 保持一致：
     * 根 pom 声明了 {@code <modules>} 时只扫这些模块，否则退回全量扫描。
     */
    private static List<Path> resolveScanBases(Path root) {
        Path pom = root.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return List.of(root);
        }
        List<Path> bases = new ArrayList<>();
        try {
            String content = Files.readString(pom, StandardCharsets.UTF_8);
            Matcher m = MODULE_PATTERN.matcher(content);
            while (m.find()) {
                Path module = root.resolve(m.group(1).trim()).normalize();
                if (Files.isDirectory(module) && !module.equals(root)) {
                    bases.add(module);
                }
            }
        } catch (IOException ignored) {
            return List.of(root);
        }
        return bases.isEmpty() ? List.of(root) : bases;
    }

    private static void collectSourceRoots(Path root, List<Path> out) throws IOException {
        try (Stream<Path> stream = Files.walk(root, 16)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (!Files.isDirectory(p) || hasSkippedAncestor(root, p)) {
                    continue;
                }
                if (!"java".equals(p.getFileName().toString())) {
                    continue;
                }
                Path parent = p.getParent();
                if (parent == null || !"main".equals(parent.getFileName().toString())) {
                    continue;
                }
                Path grand = parent.getParent();
                if (grand != null && "src".equals(grand.getFileName().toString())) {
                    out.add(p);
                }
            }
        }
    }

    private static boolean hasSkippedAncestor(Path root, Path path) {
        Path current = path;
        while (current != null && !current.equals(root)) {
            if (SKIP_DIRS.contains(current.getFileName().toString())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    // ---------------------------------------------------------------- 分析

    /**
     * 自动推断聚合深度：取"绝大多数类都认同"的包前缀长度 +1。
     *
     * <p>不用严格公共前缀，是因为真实项目里常有少量离群包（脚手架、示例、默认包），
     * 一个离群包就能把公共前缀打成 0，深度退化成 1，整份报告失去区分度。
     * 这里按类数量加权，只有某一段在 {@code MAJORITY} 以上的类中一致才继续下探。
     */
    private static int autoDepth(List<ClassInfo> classes) {
        List<String[]> segments = new ArrayList<>();
        for (ClassInfo c : classes) {
            if (!c.packageName.isEmpty()) {
                segments.add(c.packageName.split("\\."));
            }
        }
        if (segments.isEmpty()) {
            return 1;
        }
        int common = 0;
        while (common < 8) {
            Map<String, Integer> counter = new HashMap<>();
            int voted = 0;
            for (String[] segs : segments) {
                if (common >= segs.length) {
                    continue;
                }
                counter.merge(segs[common], 1, Integer::sum);
                voted++;
            }
            if (voted < segments.size() / 2) {
                break;
            }
            int best = counter.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (best < segments.size() * MAJORITY) {
                break;
            }
            common++;
        }
        return Math.max(2, Math.min(common + 1, 8));
    }

    private static String aggregate(String packageName, int depth) {
        if (packageName.isEmpty()) {
            return "(default)";
        }
        String[] segs = packageName.split("\\.");
        int n = Math.min(depth, segs.length);
        return String.join(".", Arrays.copyOf(segs, n));
    }

    private static Analysis analyze(List<ClassInfo> classes, int depth, int top) {
        Set<String> allClasses = new HashSet<>();
        for (ClassInfo c : classes) {
            allClasses.add(c.fqcn);
        }
        Map<String, Set<String>> classesByPackage = new HashMap<>();
        for (ClassInfo c : classes) {
            classesByPackage
                    .computeIfAbsent(c.packageName, k -> new LinkedHashSet<>())
                    .add(c.fqcn);
        }

        Map<String, PackageNode> nodes = new LinkedHashMap<>();

        for (ClassInfo c : classes) {
            String pkg = aggregate(c.packageName, depth);
            PackageNode node = nodes.computeIfAbsent(pkg, PackageNode::new);
            node.classes.add(c);

            if (containsAny(c.annotations, HTTP_ANNOTATIONS)) {
                node.hasHttp = true;
            }
            if (containsAny(c.annotations, JPA_ANNOTATIONS)) {
                node.hasJpa = true;
            }
            if (containsAny(c.annotations, DAO_ANNOTATIONS)) {
                node.hasDao = true;
            }
            if (containsAny(c.annotations, CONFIG_ANNOTATIONS)) {
                node.hasConfig = true;
            }
        }

        int edgeCount = 0;
        for (ClassInfo c : classes) {
            String from = aggregate(c.packageName, depth);
            for (String rawImport : c.imports) {
                if (rawImport.endsWith(".*")) {
                    String wildcardPkg = rawImport.substring(0, rawImport.length() - 2);
                    for (String fqcn : classesByPackage.getOrDefault(wildcardPkg, Set.of())) {
                        String to = aggregate(packageOf(fqcn), depth);
                        if (!to.equals(from)) {
                            if (nodes.get(from).fanOut.add(to)) {
                                edgeCount++;
                            }
                            nodes.get(to).fanIn.add(from);
                            nodes.get(to).contractClasses
                                    .computeIfAbsent(fqcn, k -> new LinkedHashSet<>()).add(from);
                        }
                    }
                    continue;
                }
                String resolved = resolveInternal(rawImport, allClasses);
                if (resolved == null) {
                    String group = externalGroup(rawImport);
                    if (group != null) {
                        nodes.get(from).externalGroups.add(group);
                    }
                    continue;
                }
                String to = aggregate(packageOf(resolved), depth);
                if (to.equals(from)) {
                    continue;
                }
                if (nodes.get(from).fanOut.add(to)) {
                    edgeCount++;
                }
                nodes.get(to).fanIn.add(from);
                nodes.get(to).contractClasses
                        .computeIfAbsent(resolved, k -> new LinkedHashSet<>()).add(from);
            }
        }

        List<PackageNode> all = new ArrayList<>(nodes.values());
        detectCycles(all);

        for (PackageNode node : all) {
            node.score = score(node);
        }

        Analysis analysis = new Analysis();
        analysis.all = all;
        analysis.classCount = classes.size();
        analysis.edgeCount = edgeCount;

        List<PackageNode> sizable = new ArrayList<>();
        for (PackageNode node : all) {
            if (node.classes.size() >= MIN_CLASS_COUNT) {
                sizable.add(node);
            }
        }
        sizable.sort(Comparator.comparingDouble((PackageNode n) -> n.score).reversed());

        for (PackageNode node : sizable) {
            if (node.fanIn.size() >= 4 && node.score < 40) {
                analysis.shared.add(node);
            } else {
                analysis.candidates.add(node);
            }
        }
        if (analysis.candidates.size() > top) {
            analysis.candidates.subList(top, analysis.candidates.size()).clear();
        }
        if (analysis.shared.size() > 5) {
            analysis.shared.subList(5, analysis.shared.size()).clear();
        }
        return analysis;
    }

    /** 只有被 4 个以上包依赖且评分很低的才算共享层，避免误伤真正的候选。 */
    private static double score(PackageNode node) {
        double score = 60.0;
        score -= node.fanIn.size() * 6.0;
        score -= node.fanOut.size() * 3.5;
        score -= node.externalGroups.size() * 0.7;
        score += Math.min(node.classes.size(), 30) * 0.5;
        if (node.hasJpa) {
            score -= 12.0;
        }
        if (node.hasDao) {
            score -= 8.0;
        }
        if (node.hasConfig) {
            score -= 6.0;
        }
        if (node.hasHttp) {
            score -= 4.0;
        }
        if (node.inCycle) {
            score -= 15.0;
        }
        int hotContracts = 0;
        for (Set<String> referrers : node.contractClasses.values()) {
            if (referrers.size() >= 3) {
                hotContracts++;
            }
        }
        score -= hotContracts * 10.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /** 找出参与循环依赖的包（简易 DFS，包数量级不大，O(V*E) 可接受）。 */
    private static void detectCycles(List<PackageNode> nodes) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (PackageNode node : nodes) {
            graph.put(node.name, node.fanOut);
        }
        for (PackageNode node : nodes) {
            if (!reaches(graph, node.name, node.name)) {
                continue;
            }
            node.inCycle = true;
            for (String to : node.fanOut) {
                if (reaches(graph, to, node.name)) {
                    node.cyclePartners.add(to);
                }
            }
        }
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String target) {
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>(graph.getOrDefault(from, Set.of()));
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            if (current.equals(target)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            queue.addAll(graph.getOrDefault(current, Set.of()));
        }
        return false;
    }

    private static boolean containsAny(Set<String> annotations, Set<String> targets) {
        for (String a : annotations) {
            if (targets.contains(a)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveInternal(String rawImport, Set<String> allClasses) {
        if (allClasses.contains(rawImport)) {
            return rawImport;
        }
        // 支持内部类引用：a.b.Outer.Inner 归到 a.b.Outer
        String candidate = rawImport;
        int idx = candidate.lastIndexOf('.');
        while (idx > 0) {
            candidate = candidate.substring(0, idx);
            if (allClasses.contains(candidate)) {
                return candidate;
            }
            idx = candidate.lastIndexOf('.');
        }
        return null;
    }

    private static String packageOf(String fqcn) {
        int idx = fqcn.lastIndexOf('.');
        return idx < 0 ? "" : fqcn.substring(0, idx);
    }

    private static String externalGroup(String rawImport) {
        String[] segs = rawImport.split("\\.");
        if (segs.length == 0) {
            return null;
        }
        if (segs.length == 1) {
            return segs[0];
        }
        return segs[0] + "." + segs[1];
    }

    // ---------------------------------------------------------------- 输出

    private static void printReport(Path root, Analysis analysis, int depth, int top) {
        System.out.println();
        System.out.println("brick-bootkit 业务切片分析器");
        System.out.println("目标项目: " + root);
        System.out.println("包聚合深度: " + depth + " 段");
        System.out.println("=".repeat(72));
        System.out.println();
        System.out.println("扫描到 " + analysis.classCount + " 个类，"
                + analysis.all.size() + " 个包，"
                + analysis.edgeCount + " 条包间依赖边");
        if (analysis.all.size() <= 2) {
            System.out.println();
            System.out.println("注意：当前聚合后只剩 " + analysis.all.size()
                    + " 个包，粒度太粗，排序结论基本没有参考价值。");
            System.out.println("      试试 --depth " + (depth + 1) + " 做更细的聚合。");
        }
        System.out.println();

        if (analysis.candidates.isEmpty()) {
            System.out.println("没有找到合适的候选切片。");
            if (analysis.classCount < MIN_CLASS_COUNT) {
                System.out.println("原因：项目规模太小（" + analysis.classCount + " 个类），"
                        + "不足 " + MIN_CLASS_COUNT + " 个类的包没有切片价值。");
            } else if (!analysis.shared.isEmpty()) {
                System.out.println("原因：达到一定规模的包全都被大量依赖（共享层），"
                        + "没有低耦合的业务包。");
                System.out.println("建议：先用 --depth 调粗或调细聚合粒度再看一次；"
                        + "若结论不变，说明需要先做解耦重构。");
            } else {
                System.out.println("原因：没有任何包达到 " + MIN_CLASS_COUNT
                        + " 个类，通常是包结构过于扁平。");
                System.out.println("建议：试试更小的 --depth " + Math.max(1, depth - 1)
                        + " 做更粗的聚合。");
            }
            System.out.println();
            return;
        }

        System.out.println("候选切片（按适宜度排序，取前 " + top + " 个）");
        System.out.println();
        int rank = 1;
        for (PackageNode node : analysis.candidates) {
            printCandidate(rank++, node);
        }

        if (!analysis.shared.isEmpty()) {
            System.out.println("-".repeat(72));
            System.out.println("不建议作为切片（共享层 / 基础设施）");
            System.out.println();
            for (PackageNode node : analysis.shared) {
                System.out.println("  " + node.name
                        + "   被 " + node.fanIn.size() + " 个包依赖，类 " + node.classes.size() + " 个");
                System.out.println("      -> 适合下沉为共享库由主程序提供，而不是切进插件；"
                        + "插件通过主程序暴露的接口访问");
            }
            System.out.println();
        }

        List<PackageNode> cycles = new ArrayList<>();
        for (PackageNode node : analysis.all) {
            if (node.inCycle) {
                cycles.add(node);
            }
        }
        if (!cycles.isEmpty()) {
            System.out.println("-".repeat(72));
            System.out.println("参与循环依赖的包（" + cycles.size() + " 个）");
            System.out.println();
            for (PackageNode node : cycles) {
                if (node.cyclePartners.isEmpty()) {
                    System.out.println("  " + node.name
                            + "   自身成环（可能是聚合后同包内部的相互引用）");
                } else {
                    System.out.println("  " + node.name + "  <->  "
                            + String.join(", ", node.cyclePartners));
                }
            }
            System.out.println("  这些包无法单独切出：切走任何一方，另一方都会编译不过。");
            System.out.println("  解环办法通常是把双方共用的类型抽到第三个包（或共享库），改成单向依赖。");
            System.out.println();
        }

        System.out.println("-".repeat(72));
        System.out.println("怎么用这份报告");
        System.out.println("  1. 优先从评分最高的包开始，它的外部耦合最小，切片代价最低");
        System.out.println("  2. 「对外契约类」就是切片后必须抽象成接口暴露给主程序的部分");
        System.out.println("  3. 先用 preflight 确认项目可接入，再按 L0/L1/L2 逐级验证切片");
        System.out.println("  4. 评分只是排序依据，最终边界仍要结合业务语义判断");
        if (depth < 8) {
            System.out.println("  5. 候选粒度不合适时用 --depth " + (depth + 1)
                    + " 看更细一层的拆分，或用更小的深度看更粗的模块划分");
        }
        System.out.println();
    }

    private static void printCandidate(int rank, PackageNode node) {
        System.out.println("  " + rank + ". " + node.name + "   评分 " + String.format("%.0f", node.score));
        System.out.println("     类 " + node.classes.size() + " 个 | 依赖 "
                + node.fanOut.size() + " 个包 | 被 " + node.fanIn.size() + " 个包依赖 | 外部库 "
                + node.externalGroups.size() + " 组");
        if (!node.fanOut.isEmpty()) {
            System.out.println("     需要主程序提供: " + brief(node.fanOut));
        } else {
            System.out.println("     需要主程序提供: 无，可独立成为一个插件");
        }
        if (!node.fanIn.isEmpty()) {
            System.out.println("     切出后主程序需改: " + brief(node.fanIn));
        }

        if (node.contractClasses.isEmpty()) {
            System.out.println("     对外契约类: 无。切片后无需向主程序暴露接口，独立性最好。");
        } else {
            System.out.println("     对外契约类 " + node.contractClasses.size() + " 个（切片后需抽象为接口）:");
            List<Map.Entry<String, Set<String>>> entries =
                    new ArrayList<>(node.contractClasses.entrySet());
            entries.sort(Comparator.comparingInt((Map.Entry<String, Set<String>> e) -> e.getValue().size())
                    .reversed());
            int shown = Math.min(entries.size(), 5);
            for (int i = 0; i < shown; i++) {
                String simple = entries.get(i).getKey();
                int dot = simple.lastIndexOf('.');
                if (dot >= 0) {
                    simple = simple.substring(dot + 1);
                }
                System.out.println("       - " + simple
                        + "  被 " + entries.get(i).getValue().size() + " 个包引用");
            }
            if (entries.size() > shown) {
                System.out.println("       - ... 另有 " + (entries.size() - shown) + " 个");
            }
        }

        List<String> risks = new ArrayList<>();
        if (node.inCycle) {
            risks.add("存在循环依赖，需先解环");
        }
        if (node.hasJpa) {
            risks.add("含 JPA 实体，跨插件边界的持久化上下文与事务需单独设计");
        }
        if (node.hasDao) {
            risks.add("含数据访问层，需确认数据源与事务管理器归属");
        }
        if (node.hasConfig) {
            risks.add("含配置类 / 启动类，切片时需迁移自动装配元数据");
        }
        if (node.hasHttp) {
            risks.add("含 HTTP 接口，切片后路由前缀会变化，需评估调用方影响");
        }
        long hot = node.contractClasses.values().stream().filter(v -> v.size() >= 3).count();
        if (hot > 0) {
            risks.add("有 " + hot + " 个类被 3 个以上包引用，切出后调用方改动面较大");
        }
        if (risks.isEmpty()) {
            System.out.println("     风险: 无明显风险");
        } else {
            System.out.println("     风险:");
            for (String risk : risks) {
                System.out.println("       ! " + risk);
            }
        }
        System.out.println();
    }

    private static String toJson(Path root, Analysis analysis, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectPath\": ").append(quote(root.toString())).append(",\n");
        sb.append("  \"packageDepth\": ").append(depth).append(",\n");
        sb.append("  \"classCount\": ").append(analysis.classCount).append(",\n");
        sb.append("  \"packageCount\": ").append(analysis.all.size()).append(",\n");
        sb.append("  \"edgeCount\": ").append(analysis.edgeCount).append(",\n");
        sb.append("  \"candidates\": [\n");
        for (int i = 0; i < analysis.candidates.size(); i++) {
            sb.append("    ").append(nodeJson(analysis.candidates.get(i)))
                    .append(i == analysis.candidates.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ],\n");
        sb.append("  \"cyclicPackages\": [\n");
        List<String> cycles = new ArrayList<>();
        for (PackageNode node : analysis.all) {
            if (node.inCycle) {
                cycles.add("    " + quote(node.name));
            }
        }
        sb.append(String.join(",\n", cycles));
        if (!cycles.isEmpty()) {
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"sharedPackages\": [\n");
        for (int i = 0; i < analysis.shared.size(); i++) {
            sb.append("    ").append(nodeJson(analysis.shared.get(i)))
                    .append(i == analysis.shared.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static String nodeJson(PackageNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"package\": ").append(quote(node.name))
                .append(", \"score\": ").append(String.format(Locale.ROOT, "%.1f", node.score))
                .append(", \"classCount\": ").append(node.classes.size())
                .append(", \"fanIn\": ").append(node.fanIn.size())
                .append(", \"fanOut\": ").append(node.fanOut.size())
                .append(", \"externalGroups\": ").append(node.externalGroups.size())
                .append(", \"inCycle\": ").append(node.inCycle)
                .append(", \"hasHttp\": ").append(node.hasHttp)
                .append(", \"hasJpa\": ").append(node.hasJpa)
                .append(", \"hasDao\": ").append(node.hasDao)
                .append(", \"hasConfig\": ").append(node.hasConfig)
                .append(", \"dependsOn\": ").append(jsonArray(node.fanOut))
                .append(", \"usedBy\": ").append(jsonArray(node.fanIn))
                .append(", \"contractClasses\": {");
        int i = 0;
        for (Map.Entry<String, Set<String>> e : node.contractClasses.entrySet()) {
            sb.append(i == 0 ? "" : ", ")
                    .append(quote(e.getKey())).append(": ").append(e.getValue().size());
            i++;
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String jsonArray(Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(sorted.get(i)));
        }
        return sb.append("]").toString();
    }

    /** 包名列表的简要展示：去掉公共前缀只留末段，最多 6 个。 */
    private static String brief(Set<String> names) {
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.naturalOrder());
        List<String> parts = new ArrayList<>();
        for (String name : sorted) {
            int dot = name.lastIndexOf('.');
            parts.add(dot >= 0 ? name.substring(dot + 1) : name);
        }
        if (parts.size() <= 6) {
            return String.join(", ", parts);
        }
        return String.join(", ", parts.subList(0, 6)) + " 等 " + parts.size() + " 个";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }
}
