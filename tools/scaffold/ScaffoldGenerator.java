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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 插件骨架生成器。
 *
 * <p>根据 slicer 分析出的候选切片，自动生成可编译的插件模块骨架。
 * 解决"知道切哪块"到"得到能跑的插件"之间的最后一公里。
 *
 * <p>用法:
 * <pre>
 *   java ScaffoldGenerator.java /path/to/project com.oldcorp.report /path/to/output
 * </pre>
 *
 * <p>约束：零第三方依赖，只用 JDK 内置 API，可单文件源码运行。
 */
public class ScaffoldGenerator {

    private static final Set<String> SKIP_DIRS = Set.of(
            "target", "build", "out", "node_modules", ".git", ".idea", ".mvn", ".gradle");

    private static final Pattern PUBLIC_METHOD =
            Pattern.compile("(?m)^[ \\t]*public[ \\t]+(?:abstract[ \\t]+)?(?:final[ \\t]+)?"
                    + "(?!class|interface|enum|static)[ \\t]*"
                    + "([\\w<>,\\[\\]\\s.]+?)[ \\t]+(\\w+)[ \\t]*\\(([^)]*)\\)[ \\t]*(?:\\{|;|throws)");
    /** 接口中的方法可能不写 public 关键字。 */
    private static final Pattern INTERFACE_METHOD =
            Pattern.compile("(?m)^[ \\t]*"
                    + "([\\w<>,\\[\\]\\s.]+?)[ \\t]+(\\w+)[ \\t]*\\(([^)]*)\\)[ \\t]*;");
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("(?m)^[ \\t]*package[ \\t]+([\\w.]+)[ \\t]*;");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^[ \\t]*import[ \\t]+(?:static[ \\t]+)?([\\w.]+|\\w+\\.\\*)[ \\t]*;");

    public static void main(String[] args) {
        Path projectRoot = null;
        String packageName = null;
        Path outDir = null;
        boolean force = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--force".equals(arg)) {
                force = true;
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsage();
                return;
            } else if (arg.startsWith("-")) {
                System.err.println("未知参数: " + arg);
                printUsage();
                System.exit(2);
            } else if (projectRoot == null) {
                projectRoot = Paths.get(arg);
            } else if (packageName == null) {
                packageName = arg;
            } else if (outDir == null) {
                outDir = Paths.get(arg);
            }
        }

        if (projectRoot == null || packageName == null) {
            System.err.println("错误：需要提供项目路径和包名。");
            printUsage();
            System.exit(2);
        }
        projectRoot = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(projectRoot)) {
            System.err.println("目录不存在: " + projectRoot);
            System.exit(2);
        }

        if (outDir == null) {
            String suffix = packageName;
            int dot = suffix.lastIndexOf('.');
            if (dot >= 0) {
                suffix = suffix.substring(dot + 1);
            }
            outDir = Paths.get(suffix + "-plugin").toAbsolutePath().normalize();
        }

        if (Files.exists(outDir)) {
            if (!force) {
                System.err.println("输出目录已存在: " + outDir);
                System.err.println("使用 --force 覆盖，或指定其他目录。");
                System.exit(2);
            }
            try {
                deleteTree(outDir);
            } catch (IOException e) {
                System.err.println("无法清理输出目录: " + e.getMessage());
                System.exit(2);
            }
        }

        // 1. 运行 slicer 获取 JSON
        String slicerJson;
        try {
            slicerJson = runSlicer(projectRoot, packageName);
        } catch (Exception e) {
            System.err.println("运行 slicer 失败: " + e.getMessage());
            System.exit(2);
            return;
        }

        // 2. 解析 JSON 找到候选包
        JsonNode candidate = findCandidate(slicerJson, packageName);
        if (candidate == null) {
            System.err.println("在 slicer 输出中未找到包: " + packageName);
            System.err.println("请确认该包出现在 slicer 的候选列表中（用 ./slice.sh 查看）。");
            System.exit(2);
            return;
        }

        // 3. 扫描原始项目中该包下的类
        List<SourceClass> classes;
        try {
            classes = scanPackageClasses(projectRoot, packageName);
        } catch (IOException e) {
            System.err.println("扫描源码失败: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (classes.isEmpty()) {
            System.err.println("在 " + packageName + " 下未找到任何 Java 源码。");
            System.exit(2);
            return;
        }

        // 4. 读取原始 pom 提取 groupId/artifactId
        PomInfo parentPom = readParentPom(projectRoot);

        // 5. 生成骨架
        try {
            generate(projectRoot, packageName, outDir, candidate, classes, parentPom);
        } catch (IOException e) {
            System.err.println("生成骨架失败: " + e.getMessage());
            System.exit(2);
            return;
        }

        System.out.println();
        System.out.println("插件骨架已生成: " + outDir);
        System.out.println();
        System.out.println("生成内容:");
        try {
            printTree(outDir, "  ");
        } catch (IOException ignored) {
        }
        System.out.println();
        System.out.println("下一步:");
        System.out.println("  1. 检查 pom.xml 中的 groupId/artifactId/依赖是否合适");
        System.out.println("  2. 把契约接口（api/ 目录）复制到主程序，让主程序编译通过");
        System.out.println("  3. 实现 PluginInfo 中的版本、描述等元数据");
        System.out.println("  4. 在主程序 pom 中声明 api 模块依赖，插件 pom 中改为 <scope>provided</scope>");
        System.out.println("  5. mvn clean package，确认插件包能正常产出");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("用法: java ScaffoldGenerator.java <项目路径> <包名> [输出目录] [--force]");
        System.out.println();
        System.out.println("  <项目路径>   要分析的原始项目根目录");
        System.out.println("  <包名>       slicer 候选列表中的包名（如 com.oldcorp.report）");
        System.out.println("  [输出目录]   缺省为 <包名末段>-plugin/");
        System.out.println("  --force      输出目录已存在时覆盖");
        System.out.println();
        System.out.println("说明: 本工具基于 slicer 的分析结果生成插件骨架，不会修改原始项目。");
    }

    // ---------------------------------------------------------------- 外部调用 slicer

    private static String runSlicer(Path projectRoot, String targetPackage) throws Exception {
        Path slicerJava = findSlicerJava();
        if (slicerJava == null) {
            throw new IOException("未找到 SliceAnalyzer.java，请确认在 brick-bootkit 仓库下运行。");
        }

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-Dfile.encoding=UTF-8", slicerJava.toString(),
                projectRoot.toString(), "--json", "--top", "20"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("slicer 退出码 " + code + ": " + output.substring(0, Math.min(200, output.length())));
        }
        return output;
    }

    private static Path findSlicerJava() {
        Path candidate = Paths.get("tools/slicer/SliceAnalyzer.java").toAbsolutePath().normalize();
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        // 尝试从 ScaffoldGenerator.java 的位置推断
        try {
            String source = new java.io.File(ScaffoldGenerator.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsolutePath();
            Path base = Paths.get(source).getParent().getParent(); // .../tools/scaffold/ -> .../
            candidate = base.resolve("tools/slicer/SliceAnalyzer.java");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ---------------------------------------------------------------- JSON 解析（极简）

    private static JsonNode findCandidate(String json, String packageName) {
        String search = "\"package\": " + quote(packageName);
        int idx = json.indexOf(search);
        if (idx < 0) {
            return null;
        }
        int start = json.lastIndexOf('{', idx);
        int end = findMatchingBrace(json, start);
        if (start < 0 || end < 0) {
            return null;
        }
        return new JsonNode(json.substring(start, end + 1));
    }

    private static int findMatchingBrace(String s, int openPos) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            } else if (c == '"') {
                i = skipString(s, i);
            }
        }
        return -1;
    }

    private static int skipString(String s, int start) {
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return s.length() - 1;
    }

    // ---------------------------------------------------------------- 扫描原始类

    private static List<SourceClass> scanPackageClasses(Path root, String packageName) throws IOException {
        List<SourceClass> result = new ArrayList<>();
        List<Path> sourceRoots = new ArrayList<>();
        collectSourceRoots(root, sourceRoots);

        String prefix = packageName + ".";
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isRegularFile(p) || !p.getFileName().toString().endsWith(".java")) {
                        continue;
                    }
                    String content = Files.readString(p, StandardCharsets.UTF_8);
                    String pkg = extractPackage(content);
                    if (pkg == null || !(pkg.equals(packageName) || pkg.startsWith(prefix))) {
                        continue;
                    }
                    String fileName = p.getFileName().toString();
                    String simple = fileName.substring(0, fileName.lastIndexOf('.'));
                    String fqcn = pkg.isEmpty() ? simple : pkg + "." + simple;
                    SourceClass sc = new SourceClass(fqcn, pkg, simple, content, p);
                    extractPublicMethods(content, sc);
                    result.add(sc);
                }
            }
        }
        result.sort(Comparator.comparing(c -> c.fqcn));
        return result;
    }

    private static String extractPackage(String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        return m.find() ? m.group(1) : "";
    }

    private static void extractPublicMethods(String content, SourceClass sc) {
        // 检测是否是接口
        boolean isInterface = content.matches("(?s).*\\binterface\\b.*");
        Set<String> seen = new HashSet<>();

        Matcher m = PUBLIC_METHOD.matcher(content);
        while (m.find()) {
            String ret = m.group(1).trim();
            String name = m.group(2);
            String params = m.group(3).trim();
            if (skipMethod(name, sc.simpleName, params)) {
                continue;
            }
            String key = name + "(" + params + ")";
            if (seen.add(key)) {
                sc.methods.add(new MethodSig(ret, name, params));
            }
        }

        if (isInterface) {
            Matcher im = INTERFACE_METHOD.matcher(content);
            while (im.find()) {
                String ret = im.group(1).trim();
                String name = im.group(2);
                String params = im.group(3).trim();
                if (skipMethod(name, sc.simpleName, params)) {
                    continue;
                }
                String key = name + "(" + params + ")";
                if (seen.add(key)) {
                    sc.methods.add(new MethodSig(ret, name, params));
                }
            }
        }
    }

    private static boolean skipMethod(String name, String className, String params) {
        if (name.equals(className)) {
            return true;
        }
        if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) {
            if (params.isEmpty() || (params.split(",").length == 1 && params.contains(" "))) {
                return true;
            }
        }
        return Set.of("toString", "equals", "hashCode", "clone", "finalize").contains(name);
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

    // ---------------------------------------------------------------- 读取原始 pom

    private static PomInfo readParentPom(Path root) {
        Path pom = root.resolve("pom.xml");
        PomInfo info = new PomInfo();
        info.groupId = "com.generated";
        info.artifactId = "plugin";
        info.version = "1.0.0-SNAPSHOT";
        info.javaVersion = "17";
        if (!Files.isRegularFile(pom)) {
            return info;
        }
        try {
            String content = Files.readString(pom, StandardCharsets.UTF_8);
            info.groupId = extractTag(content, "groupId");
            info.artifactId = extractTag(content, "artifactId");
            info.version = extractTag(content, "version");
            String java = extractTag(content, "java.version");
            if (java != null) {
                info.javaVersion = java;
            }
        } catch (IOException ignored) {
        }
        return info;
    }

    private static String extractTag(String content, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = content.indexOf(open);
        if (start < 0) {
            return null;
        }
        int end = content.indexOf(close, start);
        if (end < 0) {
            return null;
        }
        return content.substring(start + open.length(), end).trim();
    }

    // ---------------------------------------------------------------- 生成

    private static void generate(Path projectRoot, String packageName, Path outDir,
                                 JsonNode candidate, List<SourceClass> classes, PomInfo parentPom)
            throws IOException {
        Files.createDirectories(outDir);

        String basePackage = packageName;
        String pluginSuffix = packageName;
        int dot = pluginSuffix.lastIndexOf('.');
        if (dot >= 0) {
            pluginSuffix = pluginSuffix.substring(dot + 1);
        }
        String pluginClassName = capitalize(pluginSuffix) + "PluginBootstrap";
        String pluginId = pluginSuffix + "-plugin";

        // 1. pom.xml
        writeFile(outDir.resolve("pom.xml"), buildPom(pluginSuffix, pluginId, pluginClassName,
                basePackage, parentPom, candidate));

        // 2. Bootstrap 类
        Path javaRoot = outDir.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        Files.createDirectories(javaRoot.resolve("plugin"));
        writeFile(javaRoot.resolve("plugin/" + pluginClassName + ".java"),
                buildBootstrap(basePackage, pluginClassName));

        // 3. PluginInfo
        writeFile(javaRoot.resolve("plugin/" + capitalize(pluginSuffix) + "PluginInfo.java"),
                buildPluginInfo(basePackage, pluginSuffix, candidate));

        // 4. 契约接口
        Set<String> contractClasses = candidate.contractClasses();
        if (!contractClasses.isEmpty()) {
            Files.createDirectories(javaRoot.resolve("api"));
            for (String fqcn : contractClasses) {
                String simple = simpleName(fqcn);
                SourceClass orig = findOriginalClass(classes, simple);
                String iface = buildContractInterface(basePackage, simple, orig);
                writeFile(javaRoot.resolve("api/" + simple + ".java"), iface);
            }
        }

        // 5. 业务服务 stub（如果有 Service 类，且不是契约接口本身）
        Set<String> contractSimpleNames = new HashSet<>();
        for (String fqcn : contractClasses) {
            contractSimpleNames.add(simpleName(fqcn));
        }
        for (SourceClass sc : classes) {
            if (contractSimpleNames.contains(sc.simpleName)) {
                continue; // 契约接口已在 api/ 目录生成，不再作为服务 stub
            }
            if (sc.simpleName.endsWith("Service") || sc.simpleName.endsWith("ServiceImpl")) {
                String stub = buildServiceStub(basePackage, sc, contractClasses);
                if (stub != null) {
                    writeFile(javaRoot.resolve("plugin/" + sc.simpleName + ".java"), stub);
                }
            }
        }

        // 6. spring.factories
        Path meta = outDir.resolve("src/main/resources/META-INF");
        Files.createDirectories(meta);
        writeFile(meta.resolve("spring.factories"),
                buildSpringFactories(basePackage, pluginClassName));
    }

    // ---------------------------------------------------------------- 生成内容 builders

    private static String buildPom(String suffix, String pluginId, String pluginClassName,
                                   String basePackage, PomInfo parent, JsonNode candidate) {
        String groupId = parent.groupId != null ? parent.groupId : "com.generated";
        String artifactId = suffix + "-plugin";
        String javaVersion = parent.javaVersion != null ? parent.javaVersion : "17";

        StringBuilder deps = new StringBuilder();
        // 原始依赖（从 candidate.externalGroups 推断）
        Set<String> ext = candidate.externalGroups();
        if (ext.contains("org.springframework.boot")) {
            deps.append("        <dependency>\n");
            deps.append("            <groupId>org.springframework.boot</groupId>\n");
            deps.append("            <artifactId>spring-boot-starter-web</artifactId>\n");
            deps.append("        </dependency>\n");
        }
        if (ext.contains("javax.persistence") || ext.contains("jakarta.persistence")) {
            deps.append("        <dependency>\n");
            deps.append("            <groupId>jakarta.persistence</groupId>\n");
            deps.append("            <artifactId>jakarta.persistence-api</artifactId>\n");
            deps.append("        </dependency>\n");
        }
        if (ext.contains("org.apache.ibatis")) {
            deps.append("        <dependency>\n");
            deps.append("            <groupId>org.mybatis.spring.boot</groupId>\n");
            deps.append("            <artifactId>mybatis-spring-boot-starter</artifactId>\n");
            deps.append("            <version>3.0.3</version>\n");
            deps.append("        </dependency>\n");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n");
        sb.append("                             http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n");
        sb.append("\n");
        sb.append("    <parent>\n");
        sb.append("        <groupId>org.springframework.boot</groupId>\n");
        sb.append("        <artifactId>spring-boot-starter-parent</artifactId>\n");
        sb.append("        <version>3.5.5</version>\n");
        sb.append("        <relativePath/>\n");
        sb.append("    </parent>\n");
        sb.append("\n");
        sb.append("    <groupId>").append(groupId).append("</groupId>\n");
        sb.append("    <artifactId>").append(artifactId).append("</artifactId>\n");
        sb.append("    <version>1.0.0-SNAPSHOT</version>\n");
        sb.append("    <name>").append(artifactId).append("</name>\n");
        sb.append("    <description>Auto-generated plugin scaffold for ").append(basePackage).append("</description>\n");
        sb.append("\n");
        sb.append("    <properties>\n");
        sb.append("        <java.version>").append(javaVersion).append("</java.version>\n");
        sb.append("        <brick-bootkit.version>4.0.11</brick-bootkit.version>\n");
        sb.append("    </properties>\n");
        sb.append("\n");
        sb.append("    <dependencies>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>com.zqzqq</groupId>\n");
        sb.append("            <artifactId>spring-boot3-brick-bootkit-bootstrap</artifactId>\n");
        sb.append("            <version>${brick-bootkit.version}</version>\n");
        sb.append("        </dependency>\n");
        sb.append("\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>com.zqzqq</groupId>\n");
        sb.append("            <artifactId>spring-boot3-brick-bootkit</artifactId>\n");
        sb.append("            <version>${brick-bootkit.version}</version>\n");
        sb.append("            <scope>provided</scope>\n");
        sb.append("        </dependency>\n");
        sb.append("\n");
        sb.append(deps);
        sb.append("    </dependencies>\n");
        sb.append("\n");
        sb.append("    <build>\n");
        sb.append("        <plugins>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>com.zqzqq</groupId>\n");
        sb.append("                <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>\n");
        sb.append("                <version>${brick-bootkit.version}</version>\n");
        sb.append("                <configuration>\n");
        sb.append("                    <mode>prod</mode>\n");
        sb.append("                    <pluginInfo>\n");
        sb.append("                        <id>").append(pluginId).append("</id>\n");
        sb.append("                        <bootstrapClass>").append(basePackage).append(".plugin.").append(pluginClassName).append("</bootstrapClass>\n");
        sb.append("                        <version>1.0.0</version>\n");
        sb.append("                        <provider>").append(groupId).append("</provider>\n");
        sb.append("                        <description>Generated from ").append(basePackage).append("</description>\n");
        sb.append("                    </pluginInfo>\n");
        sb.append("                    <prodConfig>\n");
        sb.append("                        <packageType>jar</packageType>\n");
        sb.append("                    </prodConfig>\n");
        sb.append("                </configuration>\n");
        sb.append("                <executions>\n");
        sb.append("                    <execution>\n");
        sb.append("                        <goals>\n");
        sb.append("                            <goal>prepare-meta</goal>\n");
        sb.append("                            <goal>repackage</goal>\n");
        sb.append("                        </goals>\n");
        sb.append("                    </execution>\n");
        sb.append("                </executions>\n");
        sb.append("            </plugin>\n");
        sb.append("        </plugins>\n");
        sb.append("    </build>\n");
        sb.append("\n");
        sb.append("</project>\n");
        return sb.toString();
    }

    private static String buildBootstrap(String basePackage, String className) {
        return "package " + basePackage + ".plugin;\n" +
                "\n" +
                "import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n" +
                "\n" +
                "@SpringBootApplication\n" +
                "public class " + className + " extends SpringPluginBootstrap {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        new " + className + "().run(args);\n" +
                "    }\n" +
                "}\n";
    }

    private static String buildPluginInfo(String basePackage, String suffix, JsonNode candidate) {
        String className = capitalize(suffix) + "PluginInfo";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".plugin;\n");
        sb.append("\n");
        sb.append("import com.zqzqq.bootkits.core.model.PluginInfo;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * 插件元数据。\n");
        sb.append(" * TODO: 请根据实际业务补全版本、描述等信息。\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" implements PluginInfo {\n");
        sb.append("\n");
        sb.append("    @Override\n");
        sb.append("    public String id() {\n");
        sb.append("        return \"").append(suffix).append("-plugin\";\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    @Override\n");
        sb.append("    public String version() {\n");
        sb.append("        return \"1.0.0\";\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    @Override\n");
        sb.append("    public String provider() {\n");
        sb.append("        return \"generated\";\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    @Override\n");
        sb.append("    public String description() {\n");
        sb.append("        return \"Generated plugin from ").append(basePackage).append("\";\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildContractInterface(String basePackage, String simpleName, SourceClass orig) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".api;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * 契约接口：切片后暴露给主程序的公共契约。\n");
        sb.append(" * TODO: 请根据实际业务调整方法签名。\n");
        sb.append(" */\n");
        sb.append("public interface ").append(simpleName).append(" {\n");
        if (orig != null && !orig.methods.isEmpty()) {
            for (MethodSig m : orig.methods) {
                sb.append("\n");
                sb.append("    ").append(m.ret).append(" ").append(m.name).append("(").append(m.params).append(");\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildServiceStub(String basePackage, SourceClass sc, Set<String> contractClasses) {
        // 如果类名以 Impl 结尾，去掉 Impl 作为接口名
        String rawIfaceName = sc.simpleName;
        boolean isImpl = rawIfaceName.endsWith("Impl");
        final String ifaceName = isImpl ? rawIfaceName.substring(0, rawIfaceName.length() - 4) : rawIfaceName;

        // 检查是否有对应的契约接口
        boolean hasContract = contractClasses.stream()
                .anyMatch(c -> simpleName(c).equals(ifaceName));

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".plugin;\n");
        sb.append("\n");
        if (hasContract) {
            sb.append("import ").append(basePackage).append(".api.").append(ifaceName).append(";\n");
            sb.append("\n");
        }
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * 业务实现（从原项目迁移）。\n");
        sb.append(" * TODO: 请补全业务逻辑和依赖注入。\n");
        sb.append(" */\n");
        sb.append("@Service\n");
        sb.append("public class ").append(sc.simpleName);
        if (hasContract) {
            sb.append(" implements ").append(ifaceName);
        }
        sb.append(" {\n");

        for (MethodSig m : sc.methods) {
            sb.append("\n");
            sb.append("    @Override\n");
            sb.append("    public ").append(m.ret).append(" ").append(m.name).append("(").append(m.params).append(") {\n");
            sb.append("        // TODO: migrate business logic from original project\n");
            sb.append("        ").append(defaultReturn(m.ret)).append("\n");
            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String buildSpringFactories(String basePackage, String className) {
        return "org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\\n" +
                basePackage + ".plugin." + className + "\n";
    }

    // ---------------------------------------------------------------- 工具方法

    private static String defaultReturn(String type) {
        String t = type.trim();
        if (t.equals("void")) {
            return "return;";
        }
        if (t.equals("boolean")) {
            return "return false;";
        }
        if (t.equals("int") || t.equals("long") || t.equals("short") || t.equals("byte") || t.equals("char") || t.equals("float") || t.equals("double")) {
            return "return 0;";
        }
        if (t.startsWith("List<")) {
            return "return java.util.Collections.emptyList();";
        }
        if (t.startsWith("Set<")) {
            return "return java.util.Collections.emptySet();";
        }
        if (t.startsWith("Map<")) {
            return "return java.util.Collections.emptyMap();";
        }
        return "return null;";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String simpleName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot < 0 ? fqcn : fqcn.substring(dot + 1);
    }

    private static SourceClass findOriginalClass(List<SourceClass> classes, String simpleName) {
        for (SourceClass sc : classes) {
            if (sc.simpleName.equals(simpleName)) {
                return sc;
            }
        }
        return null;
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            Files.deleteIfExists(root);
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void printTree(Path root, String indent) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream.sorted().toList();
            for (Path p : paths) {
                if (p.equals(root)) {
                    continue;
                }
                String relative = root.relativize(p).toString();
                int depth = relative.split("/").length;
                String pad = indent + "  ".repeat(depth - 1);
                if (Files.isDirectory(p)) {
                    System.out.println(pad + p.getFileName() + "/");
                } else {
                    System.out.println(pad + p.getFileName());
                }
            }
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ---------------------------------------------------------------- 数据模型

    private static final class SourceClass {
        final String fqcn;
        final String packageName;
        final String simpleName;
        final String content;
        final Path path;
        final List<MethodSig> methods = new ArrayList<>();

        SourceClass(String fqcn, String packageName, String simpleName, String content, Path path) {
            this.fqcn = fqcn;
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.content = content;
            this.path = path;
        }
    }

    private static final class MethodSig {
        final String ret;
        final String name;
        final String params;

        MethodSig(String ret, String name, String params) {
            this.ret = ret;
            this.name = name;
            this.params = params;
        }
    }

    private static final class PomInfo {
        String groupId;
        String artifactId;
        String version;
        String javaVersion;
    }

    /**
     * 极简 JSON 节点，只支持 string 值提取。
     */
    private static final class JsonNode {
        private final String raw;

        JsonNode(String raw) {
            this.raw = raw;
        }

        String string(String key) {
            String search = "\"" + key + "\": ";
            int idx = raw.indexOf(search);
            if (idx < 0) {
                return null;
            }
            int start = idx + search.length();
            if (start >= raw.length()) {
                return null;
            }
            if (raw.charAt(start) == '"') {
                start++;
                int end = raw.indexOf('"', start);
                while (end > start && raw.charAt(end - 1) == '\\') {
                    end = raw.indexOf('"', end + 1);
                }
                return end < 0 ? raw.substring(start) : raw.substring(start, end);
            }
            // boolean / number
            int end = raw.indexOf(',', start);
            if (end < 0) {
                end = raw.indexOf('}', start);
            }
            return end < 0 ? raw.substring(start).trim() : raw.substring(start, end).trim();
        }

        Set<String> contractClasses() {
            Set<String> result = new LinkedHashSet<>();
            String search = "\"contractClasses\": {";
            int idx = raw.indexOf(search);
            if (idx < 0) {
                return result;
            }
            int start = idx + search.length();
            int end = raw.indexOf('}', start);
            if (end < 0) {
                return result;
            }
            String inner = raw.substring(start, end);
            // 简单提取 "key": value 中的 key
            int pos = 0;
            while (pos < inner.length()) {
                int q = inner.indexOf('"', pos);
                if (q < 0) {
                    break;
                }
                int q2 = inner.indexOf('"', q + 1);
                while (q2 > q + 1 && inner.charAt(q2 - 1) == '\\') {
                    q2 = inner.indexOf('"', q2 + 1);
                }
                if (q2 < 0) {
                    break;
                }
                result.add(inner.substring(q + 1, q2));
                pos = q2 + 1;
            }
            return result;
        }

        Set<String> externalGroups() {
            Set<String> result = new LinkedHashSet<>();
            String key = "\"externalGroups\": ";
            int idx = raw.indexOf(key);
            if (idx < 0) {
                return result;
            }
            int start = idx + key.length();
            // 读取数字
            int end = raw.indexOf(',', start);
            if (end < 0) {
                end = raw.indexOf('}', start);
            }
            if (end < 0) {
                return result;
            }
            try {
                int count = Integer.parseInt(raw.substring(start, end).trim());
                // 我们无法从单个节点知道具体是哪些组，返回空即可
                // 实际生成器会重新扫描源码中的 import 来推断
            } catch (NumberFormatException ignored) {
            }
            return result;
        }
    }
}
