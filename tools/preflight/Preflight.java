import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * brick-bootkit 接入前体检器（preflight）。
 *
 * <p>设计目标：在旧项目「不改一行代码、不加任何依赖」的前提下，判断它能不能接入插件框架，
 * 以及推荐用什么级别接入。用于解决「必须先接入才能自检」的鸡生蛋问题。
 *
 * <p>约束：零第三方依赖，只用 JDK 内置 API，可单文件源码运行。
 *
 * <pre>
 *   java tools/preflight/Preflight.java /path/to/your/project
 *   java tools/preflight/Preflight.java /path/to/your/project --json
 * </pre>
 */
public class Preflight {

    /** 框架版本，仅用于报告展示，需随发版同步。 */
    private static final String KIT_VERSION = "4.0.11";
    private static final int MIN_JAVA = 17;
    private static final int MIN_BOOT_MAJOR = 3;

    private static final Set<String> SKIP_DIRS = Set.of(
            "target", "build", "out", "node_modules", ".git", ".idea", ".mvn", ".gradle");

    private static final java.util.regex.Pattern BOOT_ANNOTATION =
            java.util.regex.Pattern.compile(
                    "(?m)^[ \\t]*@(SpringBootApplication|EnableAutoConfiguration)\\b");

    private enum Level { BLOCK, WARN, OK, INFO }

    private static final class Finding {
        final Level level;
        final String code;
        final String message;
        final String suggestion;

        Finding(Level level, String code, String message, String suggestion) {
            this.level = level;
            this.code = code;
            this.message = message;
            this.suggestion = suggestion;
        }
    }

    private static final class MainClass {
        final Path file;
        final String packageName;
        final String className;
        final boolean multiScan;

        MainClass(Path file, String packageName, String className, boolean multiScan) {
            this.file = file;
            this.packageName = packageName;
            this.className = className;
            this.multiScan = multiScan;
        }

        String fqcn() {
            return packageName.isEmpty() ? className : packageName + "." + className;
        }
    }

    private static final class PomInfo {
        String bootVersion;
        String javaVersion;
        String packaging = "jar";
        String parentArtifactId;
        boolean springBootManaged;
        final List<String> modules = new ArrayList<>();
        final Set<String> dependencies = new LinkedHashSet<>();
        final Map<String, String> properties = new LinkedHashMap<>();
        boolean webflux;
        boolean servletWeb;
        boolean security;
    }

    public static void main(String[] args) {
        Path projectRoot = null;
        boolean json = false;

        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                continue;
            }
            if ("--json".equals(arg)) {
                json = true;
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

        List<Finding> findings = new ArrayList<>();
        PomInfo pom = new PomInfo();
        List<MainClass> mainClasses = new ArrayList<>();
        boolean gradle = false;
        boolean multiModule = false;

        Path pomFile = projectRoot.resolve("pom.xml");
        boolean hasPom = Files.isRegularFile(pomFile);
        gradle = Files.isRegularFile(projectRoot.resolve("build.gradle"))
                || Files.isRegularFile(projectRoot.resolve("build.gradle.kts"));

        if (hasPom) {
            try {
                parsePom(pomFile, pom, true);
                multiModule = !pom.modules.isEmpty();
                for (Path sub : expandModules(projectRoot, pom.modules)) {
                    parsePom(sub, pom, false);
                }
                resolvePlaceholders(pom);
            } catch (Exception e) {
                findings.add(new Finding(Level.WARN, "PF000",
                        "pom.xml 解析失败: " + e.getMessage(),
                        "本工具只做静态解析，解析失败不影响接入，但下面的自动判断可能不准确，请人工核对"));
            }
        } else if (gradle) {
            findings.add(new Finding(Level.WARN, "PF011",
                    "检测到 Gradle 项目，当前不在支持矩阵内",
                    "官方仅验证 Maven 打包器；Gradle 项目请先用一个小插件验证打包链路"));
        } else {
            findings.add(new Finding(Level.BLOCK, "PF001",
                    "未找到 pom.xml 或 build.gradle",
                    "请在项目根目录执行，或显式传入项目路径"));
        }

        List<Path> scanRoots = new ArrayList<>();
        if (multiModule) {
            for (Path sub : expandModules(projectRoot, pom.modules)) {
                Path dir = sub.getParent();
                if (dir != null) {
                    scanRoots.add(dir);
                }
            }
        }
        if (scanRoots.isEmpty()) {
            scanRoots.add(projectRoot);
        }

        try {
            mainClasses = findMainClasses(scanRoots);
        } catch (IOException e) {
            findings.add(new Finding(Level.WARN, "PF023",
                    "源码扫描失败: " + e.getMessage(),
                    "请人工确认启动类位置，并显式配置 plugin.mainPackage"));
        }

        evaluate(findings, pom, mainClasses, hasPom, gradle, multiModule);

        Advice advice = buildAdvice(findings, pom, mainClasses);
        if (json) {
            System.out.println(toJson(projectRoot, findings, pom, mainClasses, advice));
        } else {
            printReport(projectRoot, findings, pom, mainClasses, advice);
        }

        for (Finding f : findings) {
            if (f.level == Level.BLOCK) {
                System.exit(1);
            }
        }
    }

    private static void printUsage() {
        System.out.println("用法: java Preflight.java <项目路径> [--json]");
        System.out.println();
        System.out.println("  <项目路径>  要体检的项目根目录，缺省为当前目录");
        System.out.println("  --json      以 JSON 输出，便于接入 CI");
        System.out.println();
        System.out.println("退出码: 0=无阻断项  1=存在阻断项  2=参数或路径错误");
    }

    // ---------------------------------------------------------------- 规则判断

    private static void evaluate(List<Finding> findings,
                                 PomInfo pom,
                                 List<MainClass> mainClasses,
                                 boolean hasPom,
                                 boolean gradle,
                                 boolean multiModule) {

        if (hasPom && pom.parentArtifactId == null && !pom.springBootManaged) {
            findings.add(new Finding(Level.BLOCK, "PF003",
                    "未识别到 Spring Boot 依赖管理",
                    "brick-bootkit 只支持 Spring Boot 项目，请确认是否继承了 spring-boot-starter-parent "
                            + "或 import 了 spring-boot-dependencies"));
        } else if (pom.bootVersion != null) {
            Integer major = majorOf(pom.bootVersion);
            if (major == null) {
                findings.add(new Finding(Level.WARN, "PF003",
                        "无法解析 Spring Boot 版本: " + pom.bootVersion,
                        "请人工确认版本不低于 3.0"));
            } else if (major < MIN_BOOT_MAJOR) {
                findings.add(new Finding(Level.BLOCK, "PF002",
                        "Spring Boot " + pom.bootVersion + " 低于基线要求",
                        "brick-bootkit " + KIT_VERSION + " 面向 Spring Boot 3.x；"
                                + "Spring Boot 2.x 项目请改用 brick-bootkit 3.x 或先升级框架"));
            } else {
                findings.add(new Finding(Level.OK, "PF002",
                        "Spring Boot " + pom.bootVersion + " 满足基线要求",
                        "基线为 3.5.5，低于该版本建议升级后再接入"));
            }
        }

        Integer javaMajor = javaMajorOf(pom.javaVersion);
        if (javaMajor == null) {
            String runtime = System.getProperty("java.version", "未知");
            findings.add(new Finding(Level.WARN, "PF005",
                    "未能从 pom 中确定 Java 版本（当前运行 JVM: " + runtime + "）",
                    "请在 pom 中显式配置 java.version 或 maven.compiler.release，便于准确判断"));
        } else if (javaMajor < MIN_JAVA) {
            findings.add(new Finding(Level.BLOCK, "PF004",
                    "Java " + pom.javaVersion + " 低于基线要求",
                    "基线为 JDK 17；无法升级时请改用 brick-bootkit 3.x，且不在官方回归范围内"));
        } else {
            findings.add(new Finding(Level.OK, "PF004",
                    "Java " + pom.javaVersion + " 满足基线要求", "无需处理"));
        }

        if ("war".equalsIgnoreCase(pom.packaging)) {
            findings.add(new Finding(Level.WARN, "PF010",
                    "packaging=war，可能是外置容器部署",
                    "外置 Tomcat 下类加载层次不受框架控制，插件隔离行为需额外验证；建议先改为 jar 内嵌容器验证"));
        } else if ("pom".equalsIgnoreCase(pom.packaging)) {
            findings.add(new Finding(Level.WARN, "PF012",
                    "根工程 packaging=pom，这是聚合工程而不是宿主工程",
                    "已合并解析各子模块，但结论会被稀释；请对真正承载启动类的模块单独再执行一次体检"));
        } else if (hasPom) {
            findings.add(new Finding(Level.OK, "PF010",
                    "packaging=" + pom.packaging + "，符合常规接入形态", "无需处理"));
        }

        if (mainClasses.isEmpty()) {
            findings.add(new Finding(Level.WARN, "PF020",
                    "未扫描到 @SpringBootApplication / @EnableAutoConfiguration 启动类",
                    "框架的 mainPackage 自动推断依赖启动类结构；找不到时请显式配置 plugin.mainPackage"));
        } else if (mainClasses.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (MainClass mc : mainClasses) {
                sb.append(mc.fqcn()).append(" ");
            }
            findings.add(new Finding(Level.WARN, "PF021",
                    "扫描到 " + mainClasses.size() + " 个启动类: " + sb.toString().trim(),
                    "多启动类会让 mainPackage 推断产生歧义，请显式配置 plugin.mainPackage"));
        } else {
            findings.add(new Finding(Level.OK, "PF020",
                    "启动类唯一: " + mainClasses.get(0).fqcn(),
                    "mainPackage 可自动推断，仍建议在配置中显式写出"));
        }

        boolean anyMultiScan = mainClasses.stream().anyMatch(mc -> mc.multiScan);
        if (anyMultiScan) {
            findings.add(new Finding(Level.WARN, "PF022",
                    "启动类配置了多个 scanBasePackages",
                    "框架推断 mainPackage 时只取自动装配包的第一个，可能过宽或过窄；请显式配置 plugin.mainPackage"));
        }

        if (multiModule) {
            findings.add(new Finding(Level.WARN, "PF040",
                    "多模块项目，已合并解析 " + pom.modules.size() + " 个子模块",
                    "请确认插件依赖只放在会被打包的模块中，并把 pluginPath 指向部署目录下的固定路径"));
        }

        if (pom.webflux && !pom.servletWeb) {
            findings.add(new Finding(Level.WARN, "PF030",
                    "只检测到 spring-boot-starter-webflux",
                    "Web 控制台与插件 MVC 路由基于 Servlet 栈；纯 WebFlux 宿主请勿引入 web 模块"));
        }

        if (pom.security) {
            findings.add(new Finding(Level.WARN, "PF031",
                    "检测到 Spring Security",
                    "接入 Web 控制台后需放行 /plugins-web/**，或实现 PluginWebAuthorizer 做鉴权委托"));
        }

        for (String dep : pom.dependencies) {
            String lower = dep.toLowerCase(Locale.ROOT);
            if (lower.contains("skywalking") || lower.contains("arthas") || lower.contains("jacoco")) {
                findings.add(new Finding(Level.WARN, "PF033",
                        "检测到 Java Agent 相关依赖: " + dep,
                        "Agent 会改写字节码或插入自定义 ClassLoader，与插件类加载器叠加时风险较高，务必在预发环境验证"));
            }
            if (lower.contains("fastjson") && !lower.contains("fastjson2")) {
                findings.add(new Finding(Level.WARN, "PF032",
                        "检测到 fastjson 1.x: " + dep,
                        "fastjson 1.x 与类隔离组合时问题较多，建议升级到 fastjson2 或改用 Jackson"));
            }
            if (lower.contains("dubbo") || lower.contains("shardingsphere")) {
                findings.add(new Finding(Level.WARN, "PF034",
                        "检测到自带类加载体系的组件: " + dep,
                        "该组件有独立的 ClassLoader 或 SPI 机制，插件中使用时需显式处理上下文类加载器"));
            }
        }
    }

    // ---------------------------------------------------------------- 建议生成

    private static final class Advice {
        String mainPackage = "";
        String mainPackageReason = "";
        String adoptionLevel = "L0";
        String levelReason = "";
        int block;
        int warn;
    }

    private static Advice buildAdvice(List<Finding> findings, PomInfo pom, List<MainClass> mainClasses) {
        Advice advice = new Advice();
        for (Finding f : findings) {
            if (f.level == Level.BLOCK) {
                advice.block++;
            } else if (f.level == Level.WARN) {
                advice.warn++;
            }
        }

        if (mainClasses.size() == 1) {
            advice.mainPackage = mainClasses.get(0).packageName;
            advice.mainPackageReason = "启动类所在包，框架可据此推断；建议显式配置以避免启动类结构调整后失效";
        } else if (mainClasses.size() > 1) {
            advice.mainPackage = commonPrefix(mainClasses);
            advice.mainPackageReason = "多个启动类的公共包前缀，仅供参考，强烈建议显式指定其中一个业务主包";
        } else {
            advice.mainPackage = "";
            advice.mainPackageReason = "未扫描到启动类，需要人工确认后显式配置 plugin.mainPackage";
        }

        if (advice.block > 0) {
            advice.adoptionLevel = "NONE";
            advice.levelReason = "存在阻断项，先解决阻断项再评估接入";
        } else if (advice.warn > 0) {
            advice.adoptionLevel = "L0";
            advice.levelReason = "存在需要验证的风险项，建议先以影子模式接入，确认无误后再升级";
        } else {
            advice.adoptionLevel = "L1";
            advice.levelReason = "未发现风险项，可从观察模式起步，验证插件能被正确解析";
        }
        return advice;
    }

    private static String commonPrefix(List<MainClass> mainClasses) {
        String prefix = mainClasses.get(0).packageName;
        for (int i = 1; i < mainClasses.size(); i++) {
            String other = mainClasses.get(i).packageName;
            int n = Math.min(prefix.length(), other.length());
            int idx = 0;
            while (idx < n && prefix.charAt(idx) == other.charAt(idx)) {
                idx++;
            }
            prefix = prefix.substring(0, idx);
            while (prefix.endsWith(".")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }

    // ---------------------------------------------------------------- 报告输出

    private static void printReport(Path projectRoot,
                                    List<Finding> findings,
                                    PomInfo pom,
                                    List<MainClass> mainClasses,
                                    Advice advice) {
        List<Finding> blocks = byLevel(findings, Level.BLOCK);
        List<Finding> warns = byLevel(findings, Level.WARN);
        List<Finding> oks = byLevel(findings, Level.OK);

        System.out.println();
        System.out.println("brick-bootkit preflight  |  框架基线 " + KIT_VERSION
                + "  (JDK " + MIN_JAVA + " / Spring Boot 3.x)");
        System.out.println("目标项目: " + projectRoot);
        System.out.println("=".repeat(72));
        System.out.println();
        System.out.println("阻断 " + blocks.size() + "    警告 " + warns.size() + "    通过 " + oks.size());
        System.out.println();

        if (!blocks.isEmpty()) {
            System.out.println("阻断项（必须解决后才能接入）");
            for (Finding f : blocks) {
                printFinding(f);
            }
            System.out.println();
        }
        if (!warns.isEmpty()) {
            System.out.println("警告项（不阻断接入，但需要验证）");
            for (Finding f : warns) {
                printFinding(f);
            }
            System.out.println();
        }
        if (!oks.isEmpty()) {
            System.out.println("已通过");
            for (Finding f : oks) {
                printFinding(f);
            }
            System.out.println();
        }

        System.out.println("-".repeat(72));
        System.out.println("建议配置");
        System.out.println("  plugin.mainPackage   " + (advice.mainPackage.isEmpty() ? "(需人工确认)" : advice.mainPackage));
        System.out.println("                       " + advice.mainPackageReason);
        System.out.println("  推荐接入级别          " + advice.adoptionLevel + "   " + advice.levelReason);
        System.out.println("-".repeat(72));
        System.out.println();
        System.out.println("下一步");
        if ("NONE".equals(advice.adoptionLevel)) {
            System.out.println("  1. 先解决上面的阻断项，再重新执行本体检");
            System.out.println("  2. 若短期内无法升级 JDK / Spring Boot，请改用 brick-bootkit 3.x，"
                    + "并注意其不在官方回归范围内");
            System.out.println("  3. 阻断项无法解决时，不建议在当前项目上接入");
        } else {
            System.out.println("  1. 引入 spring-boot3-brick-bootkit:" + KIT_VERSION);
            System.out.println("  2. 按推荐级别配置 plugin.autoLoadPlugins / plugin.autoStartPlugins");
            System.out.println("  3. 启动宿主，确认无异常后再升级接入级别");
            System.out.println("  4. 接入 web 模块后访问 GET /plugins-web/api/doctor 做运行时复核");
        }
        System.out.println();
    }

    private static void printFinding(Finding f) {
        String tag = f.level == Level.BLOCK ? "[阻断]" : (f.level == Level.WARN ? "[警告]" : "[通过]");
        System.out.println("  " + tag + " " + f.code + "  " + f.message);
        System.out.println("         -> " + f.suggestion);
    }

    private static List<Finding> byLevel(List<Finding> findings, Level level) {
        List<Finding> result = new ArrayList<>();
        for (Finding f : findings) {
            if (f.level == level) {
                result.add(f);
            }
        }
        return result;
    }

    private static String toJson(Path projectRoot,
                                 List<Finding> findings,
                                 PomInfo pom,
                                 List<MainClass> mainClasses,
                                 Advice advice) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"kitVersion\": \"").append(KIT_VERSION).append("\",\n");
        sb.append("  \"projectPath\": ").append(quote(projectRoot.toString())).append(",\n");
        sb.append("  \"springBootVersion\": ").append(quote(pom.bootVersion)).append(",\n");
        sb.append("  \"javaVersion\": ").append(quote(pom.javaVersion)).append(",\n");
        sb.append("  \"packaging\": ").append(quote(pom.packaging)).append(",\n");
        sb.append("  \"blockCount\": ").append(advice.block).append(",\n");
        sb.append("  \"warnCount\": ").append(advice.warn).append(",\n");
        sb.append("  \"mainPackage\": ").append(quote(advice.mainPackage)).append(",\n");
        sb.append("  \"adoptionLevel\": ").append(quote(advice.adoptionLevel)).append(",\n");
        sb.append("  \"mainClasses\": [");
        for (int i = 0; i < mainClasses.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(mainClasses.get(i).fqcn()));
        }
        sb.append("],\n");
        sb.append("  \"findings\": [\n");
        for (int i = 0; i < findings.size(); i++) {
            Finding f = findings.get(i);
            sb.append("    {\"level\": ").append(quote(f.level.name()))
                    .append(", \"code\": ").append(quote(f.code))
                    .append(", \"message\": ").append(quote(f.message))
                    .append(", \"suggestion\": ").append(quote(f.suggestion))
                    .append("}").append(i == findings.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
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

    // ---------------------------------------------------------------- pom 解析

    /**
     * 展开 modules 声明，返回所有子模块 pom.xml 路径。
     * 只沿声明链查找，避免把未参与构建的示例目录也当成业务模块。
     */
    private static List<Path> expandModules(Path root, List<String> modules) {
        List<Path> result = new ArrayList<>();
        List<Path> current = new ArrayList<>();
        for (String module : modules) {
            Path pom = root.resolve(module).resolve("pom.xml");
            if (Files.isRegularFile(pom)) {
                current.add(pom);
            }
        }
        result.addAll(current);
        for (int depth = 0; depth < 2 && !current.isEmpty(); depth++) {
            List<Path> next = new ArrayList<>();
            for (Path pom : current) {
                try {
                    PomInfo child = new PomInfo();
                    parsePom(pom, child, false);
                    for (String module : child.modules) {
                        Path sub = pom.getParent().resolve(module).resolve("pom.xml");
                        if (Files.isRegularFile(sub) && !result.contains(sub)) {
                            next.add(sub);
                        }
                    }
                } catch (Exception ignored) {
                    // 子模块解析失败不影响主流程
                }
            }
            result.addAll(next);
            current = next;
        }
        return result;
    }

    private static void resolvePlaceholders(PomInfo info) {
        info.bootVersion = resolveExpr(info.bootVersion, info.properties, 5);
        info.javaVersion = resolveExpr(info.javaVersion, info.properties, 5);
    }

    private static String resolveExpr(String value, Map<String, String> properties, int depth) {
        if (value == null || depth <= 0) {
            return value;
        }
        String current = value;
        for (int i = 0; i < depth; i++) {
            int start = current.indexOf("${");
            if (start < 0) {
                return current;
            }
            int end = current.indexOf('}', start);
            if (end < 0) {
                return current;
            }
            String key = current.substring(start + 2, end);
            String replacement = properties.get(key);
            if (replacement == null) {
                return current;
            }
            current = current.substring(0, start) + replacement + current.substring(end + 1);
        }
        return current;
    }

    private static void parsePom(Path pomFile, PomInfo info, boolean root) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile.toFile());
        doc.getDocumentElement().normalize();

        Element project = doc.getDocumentElement();

        Element parent = firstChild(project, "parent");
        if (parent != null) {
            info.parentArtifactId = text(parent, "artifactId");
            String version = text(parent, "version");
            if ("spring-boot-starter-parent".equals(info.parentArtifactId) && version != null) {
                info.bootVersion = version;
            }
        }

        String pkg = text(project, "packaging");
        if (root && pkg != null && !pkg.isEmpty()) {
            info.packaging = pkg;
        }

        Element properties = firstChild(project, "properties");
        if (properties != null) {
            NodeList children = properties.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String key = n.getNodeName();
                    String value = n.getTextContent() == null ? "" : n.getTextContent().trim();
                    info.properties.putIfAbsent(key, value);
                }
            }
            if (info.bootVersion == null) {
                String fromProp = info.properties.get("spring-boot.version");
                if (fromProp != null && !fromProp.isEmpty()) {
                    info.bootVersion = fromProp;
                }
            }
            if (info.javaVersion == null) {
                for (String key : List.of("java.version", "maven.compiler.release",
                        "maven.compiler.source", "maven.compiler.target")) {
                    String v = info.properties.get(key);
                    if (v != null && !v.isEmpty()) {
                        info.javaVersion = v;
                        break;
                    }
                }
            }
        }

        Element depMgmt = firstChild(project, "dependencyManagement");
        if (depMgmt != null) {
            Element deps = firstChild(depMgmt, "dependencies");
            if (deps != null) {
                for (Element dep : children(deps, "dependency")) {
                    String artifactId = text(dep, "artifactId");
                    String version = text(dep, "version");
                    if ("spring-boot-dependencies".equals(artifactId) && version != null) {
                        info.bootVersion = version;
                        info.springBootManaged = true;
                    }
                    recordDependency(info, text(dep, "groupId"), artifactId);
                }
            }
        }

        Element dependencies = firstChild(project, "dependencies");
        if (dependencies != null) {
            for (Element dep : children(dependencies, "dependency")) {
                String groupId = text(dep, "groupId");
                String artifactId = text(dep, "artifactId");
                if ("org.springframework.boot".equals(groupId) && !info.springBootManaged) {
                    info.springBootManaged = true;
                }
                recordDependency(info, groupId, artifactId);
            }
        }

        Element modules = firstChild(project, "modules");
        if (modules != null) {
            for (Element m : children(modules, "module")) {
                String name = m.getTextContent();
                if (name != null && !name.trim().isEmpty()) {
                    info.modules.add(name.trim());
                }
            }
        }
    }

    private static void recordDependency(PomInfo info, String groupId, String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return;
        }
        String key = (groupId == null ? "" : groupId + ":") + artifactId;
        info.dependencies.add(key);
        String lower = artifactId.toLowerCase(Locale.ROOT);
        if (lower.contains("webflux")) {
            info.webflux = true;
        }
        if (lower.contains("starter-web") || lower.contains("servlet")) {
            info.servletWeb = true;
        }
        if (lower.contains("security")) {
            info.security = true;
        }
    }

    private static Element firstChild(Element parent, String name) {
        NodeList list = parent.getElementsByTagName(name);
        if (list.getLength() == 0) {
            return null;
        }
        return (Element) list.item(0);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
                result.add((Element) n);
            }
        }
        return result;
    }

    private static String text(Element parent, String name) {
        NodeList list = parent.getElementsByTagName(name);
        if (list.getLength() == 0) {
            return null;
        }
        String value = list.item(0).getTextContent();
        return value == null ? null : value.trim();
    }

    // ---------------------------------------------------------------- 源码扫描

    private static List<MainClass> findMainClasses(List<Path> roots) throws IOException {
        List<Path> sourceRoots = new ArrayList<>();
        for (Path root : roots) {
            collectSourceRoots(root, sourceRoots);
        }

        List<MainClass> result = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isRegularFile(p)) {
                        continue;
                    }
                    String name = p.getFileName().toString();
                    if (!name.endsWith(".java") && !name.endsWith(".kt")) {
                        continue;
                    }
                    MainClass mc = inspectSource(p);
                    if (mc != null) {
                        result.add(mc);
                    }
                }
            }
        }
        result.sort(Comparator.comparing(mc -> mc.fqcn()));
        return result;
    }

    private static MainClass inspectSource(Path file) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        // 必须作为注解出现在行首（只允许前置空白），避免把注释和字符串常量里的同名文本当成启动类
        if (!BOOT_ANNOTATION.matcher(content).find()) {
            return null;
        }
        String packageName = "";
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
                packageName = trimmed.substring("package ".length(), trimmed.length() - 1).trim();
                break;
            }
        }
        String fileName = file.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        boolean multiScan = content.contains("scanBasePackages") || content.contains("scanBasePackageClasses");
        return new MainClass(file, packageName, className, multiScan);
    }

    private static void collectSourceRoots(Path root, List<Path> out) throws IOException {
        try (Stream<Path> stream = Files.walk(root, 14)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (!Files.isDirectory(p) || hasSkippedAncestor(root, p)) {
                    continue;
                }
                if (!"java".equals(p.getFileName().toString()) && !"kotlin".equals(p.getFileName().toString())) {
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

    // ---------------------------------------------------------------- 版本工具

    private static Integer majorOf(String version) {
        if (version == null) {
            return null;
        }
        String v = version.trim();
        if (v.startsWith("${") || v.isEmpty()) {
            return null;
        }
        int start = v.toLowerCase(Locale.ROOT).startsWith("v") ? 1 : 0;
        StringBuilder digits = new StringBuilder();
        for (int i = start; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer javaMajorOf(String version) {
        if (version == null) {
            return null;
        }
        String v = version.trim();
        if (v.startsWith("${") || v.isEmpty()) {
            return null;
        }
        if (v.contains(".") && !v.startsWith("1.")) {
            v = v.substring(0, v.indexOf('.'));
        } else if (v.startsWith("1.")) {
            String[] parts = v.split("\\.");
            if (parts.length >= 2) {
                v = parts[1];
            }
        }
        return majorOf(v);
    }
}
