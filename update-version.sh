#!/bin/bash

# Brick BootKit SpringBoot 版本号同步脚本
# 用法: ./update-version.sh <new-version>
# 示例: ./update-version.sh 4.1.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 动态检测项目根目录
detect_project_root() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    
    # 优先使用git仓库根目录
    if git rev-parse --show-toplevel > /dev/null 2>&1; then
        git rev-parse --show-toplevel
    # 其次使用脚本所在目录
    elif [ -f "$script_dir/pom.xml" ]; then
        echo "$script_dir"
    # 最后使用当前目录
    else
        pwd
    fi
}

sed_in_place() {
    local expression="$1"
    local file="$2"
    if sed --version > /dev/null 2>&1; then
        sed -i "$expression" "$file"
    else
        sed -i '' "$expression" "$file"
    fi
}

PROJECT_ROOT="$(detect_project_root)"

# 检查参数
if [ $# -ne 1 ]; then
    echo -e "${RED}错误: 请提供新版本号${NC}"
    echo -e "${YELLOW}用法: $0 <new-version>${NC}"
    echo -e "${YELLOW}示例: $0 4.1.0${NC}"
    exit 1
fi

NEW_VERSION="$1"

# 验证版本号格式 (简单的语义化版本检查)
if [[ ! $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9]+)?$ ]]; then
    echo -e "${RED}错误: 版本号格式不正确${NC}"
    echo -e "${YELLOW}示例格式: 4.0.0, 4.1.0, 4.0.1-beta${NC}"
    exit 1
fi

# 进入项目根目录
cd "$PROJECT_ROOT"

echo -e "${BLUE}=== Brick BootKit SpringBoot 版本号更新工具 ===${NC}"
echo -e "新版本号: ${GREEN}$NEW_VERSION${NC}"
echo -e "项目根目录: $PROJECT_ROOT"
echo

# 检查是否在git仓库中
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo -e "${YELLOW}警告: 当前目录不是git仓库${NC}"
fi

# 显示当前版本信息
echo -e "${BLUE}=== 当前版本信息 ===${NC}"
CURRENT_VERSION=$(grep -o '<version>[^<]*</version>' pom.xml | sed 's/<version>\([^<]*\)<\/version>/\1/' | head -1)
echo -e "当前版本: ${GREEN}$CURRENT_VERSION${NC}"
echo

# 备份当前pom.xml文件
echo -e "${YELLOW}正在备份pom.xml文件...${NC}"
cp pom.xml "pom.xml.backup.$(date +%Y%m%d_%H%M%S)"
cp "spring-boot3-brick-bootkit-core/pom.xml" "spring-boot3-brick-bootkit-core/pom.xml.backup.$(date +%Y%m%d_%H%M%S)"
echo -e "${GREEN}备份完成${NC}"
echo

# 更新根目录pom.xml中的parent版本号
echo -e "${BLUE}=== 更新根目录pom.xml ===${NC}"
if grep -q "<version>$CURRENT_VERSION</version>" pom.xml; then
    sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" pom.xml
    echo -e "${GREEN}根目录pom.xml版本号已更新${NC}"
else
    echo -e "${YELLOW}在根目录pom.xml中未找到版本号 $CURRENT_VERSION${NC}"
fi

# 更新所有子模块pom.xml中的版本号
echo -e "${BLUE}=== 更新子模块版本号 ===${NC}"

# 首先更新特殊的spring-boot3-brick-bootkit模块（如果有的话）
if [ -f "spring-boot3-brick-bootkit/pom.xml" ]; then
    echo "正在更新特殊模块: spring-boot3-brick-bootkit"
    
    # 更新模块中的version标签
    if grep -q "<version>$CURRENT_VERSION</version>" spring-boot3-brick-bootkit/pom.xml; then
        sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" spring-boot3-brick-bootkit/pom.xml
        echo -e "  ${GREEN}✓ 模块版本号已更新${NC}"
    else
        echo -e "  ${YELLOW}⚠ 未找到模块版本号 $CURRENT_VERSION${NC}"
    fi
    
    # 更新parent引用版本号
    if grep -q "<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" spring-boot3-brick-bootkit/pom.xml; then
        if grep -q "<version>$CURRENT_VERSION</version>" spring-boot3-brick-bootkit/pom.xml; then
            sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" spring-boot3-brick-bootkit/pom.xml
            echo -e "  ${GREEN}✓ 父模块引用版本号已更新${NC}"
        else
            echo -e "  ${YELLOW}⚠ 未找到父模块引用版本号 $CURRENT_VERSION${NC}"
        fi
    fi
fi

for module_dir in spring-boot3-brick-bootkit-*/; do
    if [ -d "$module_dir" ] && [ -f "$module_dir/pom.xml" ]; then
        echo "正在更新模块: $module_dir"
        
        # 更新模块中的version标签
        if grep -q "<version>$CURRENT_VERSION</version>" "$module_dir/pom.xml"; then
            sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" "$module_dir/pom.xml"
            echo -e "  ${GREEN}✓ 版本号已更新${NC}"
        else
            echo -e "  ${YELLOW}⚠ 未找到版本号 $CURRENT_VERSION${NC}"
        fi
        
        # 检查并更新parent版本号（如果引用了父模块）
        if grep -q "<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" "$module_dir/pom.xml"; then
            if grep -q "<version>$CURRENT_VERSION</version>" "$module_dir/pom.xml"; then
                sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" "$module_dir/pom.xml"
                echo -e "  ${GREEN}✓ 父模块版本号已更新${NC}"
            else
                echo -e "  ${YELLOW}⚠ 未找到父模块版本号 $CURRENT_VERSION${NC}"
            fi
        fi
    fi
done

# 更新特定文件中的版本号（如README、配置文件等）
echo -e "${BLUE}=== 更新其他文件中的版本号 ===${NC}"

# 更新README.md中的版本号（如果存在）
if [ -f "README.md" ]; then
    if grep -q "$CURRENT_VERSION" README.md; then
        sed_in_place "s/$CURRENT_VERSION/$NEW_VERSION/g" README.md
        echo -e "${GREEN}README.md版本号已更新${NC}"
    fi
fi

# 更新doc目录中的文档
if [ -d "doc" ]; then
    for doc_file in doc/*.md; do
        if [ -f "$doc_file" ] && grep -q "$CURRENT_VERSION" "$doc_file"; then
            sed_in_place "s/$CURRENT_VERSION/$NEW_VERSION/g" "$doc_file"
            echo -e "${GREEN}$doc_file 版本号已更新${NC}"
        fi
    done
fi

# 更新Maven插件描述文件中的版本号
echo -e "${YELLOW}正在更新Maven插件描述文件...${NC}"

# 定义需要更新的Maven插件描述文件
MAVEN_PLUGIN_FILES=(
    "spring-boot3-brick-bootkit-maven-packager/src/main/resources/META-INF/maven/com.gitee.starblues.springboot-plugin-maven-packager/plugin-help.xml"
    "spring-boot3-brick-bootkit-maven-packager/src/main/resources/META-INF/maven/plugin.xml"
)

for plugin_file in "${MAVEN_PLUGIN_FILES[@]}"; do
    if [ -f "$plugin_file" ]; then
        # 使用XML特定的替换，避免影响其他版本号格式
        # 更新 <version> 标签中的版本号
        if grep -q "<version>$CURRENT_VERSION</version>" "$plugin_file"; then
            sed_in_place "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/g" "$plugin_file"
            echo -e "  ${GREEN}✓ $plugin_file 版本号已更新${NC}"
        else
            echo -e "  ${YELLOW}⚠ $plugin_file 中未找到版本号 $CURRENT_VERSION${NC}"
        fi
    else
        echo -e "  ${YELLOW}⚠ 文件不存在: $plugin_file${NC}"
    fi
done

# 验证版本更新结果（不执行Maven构建）
echo
echo -e "${BLUE}=== 验证更新结果 ===${NC}"

# 检查根目录pom.xml
NEW_ROOT_VERSION=$(grep -o '<version>[^<]*</version>' pom.xml | sed 's/<version>\([^<]*\)<\/version>/\1/' | head -1)
if [ "$NEW_ROOT_VERSION" = "$NEW_VERSION" ]; then
    echo -e "根目录pom.xml: ${GREEN}✓ 版本号正确 ($NEW_VERSION)${NC}"
else
    echo -e "根目录pom.xml: ${RED}✗ 版本号不正确 (期望: $NEW_VERSION, 实际: $NEW_ROOT_VERSION)${NC}"
fi

# 检查特殊模块
if [ -f "spring-boot3-brick-bootkit/pom.xml" ]; then
    spring_boot_kit_version=$(grep -o '<version>[^<]*</version>' spring-boot3-brick-bootkit/pom.xml | sed 's/<version>\([^<]*\)<\/version>/\1/' | head -1)
    
    if [ "$spring_boot_kit_version" = "$NEW_VERSION" ]; then
        echo -e "  spring-boot3-brick-bootkit: ${GREEN}✓ $spring_boot_kit_version${NC}"
    else
        echo -e "  spring-boot3-brick-bootkit: ${RED}✗ $spring_boot_kit_version (期望: $NEW_VERSION)${NC}"
    fi
fi

# 检查子模块
echo -e "${YELLOW}检查子模块版本号...${NC}"
for module_dir in spring-boot3-brick-bootkit-*/; do
    if [ -d "$module_dir" ] && [ -f "$module_dir/pom.xml" ]; then
        module_version=$(grep -o '<version>[^<]*</version>' "$module_dir/pom.xml" | sed 's/<version>\([^<]*\)<\/version>/\1/' | head -1)
        module_name=$(basename "$module_dir")
        
        if [ "$module_version" = "$NEW_VERSION" ]; then
            echo -e "  $module_name: ${GREEN}✓ $module_version${NC}"
        else
            echo -e "  $module_name: ${RED}✗ $module_version (期望: $NEW_VERSION)${NC}"
        fi
    fi
done

# 检查Maven插件描述文件
echo -e "${YELLOW}检查Maven插件描述文件版本号...${NC}"
for plugin_file in "${MAVEN_PLUGIN_FILES[@]}"; do
    if [ -f "$plugin_file" ]; then
        plugin_version=$(grep -o "<version>[^<]*</version>" "$plugin_file" | sed 's/<version>\([^<]*\)<\/version>/\1/' | head -1)
        plugin_name=$(basename "$plugin_file")
        
        if [ "$plugin_version" = "$NEW_VERSION" ]; then
            echo -e "  $plugin_name: ${GREEN}✓ $plugin_version${NC}"
        else
            echo -e "  $plugin_name: ${RED}✗ $plugin_version (期望: $NEW_VERSION)${NC}"
        fi
    fi
done

echo
echo -e "${GREEN}✓ 版本号更新验证完成${NC}"

# 手动构建提示
echo
echo -e "${BLUE}=== 后续操作提示 ===${NC}"
echo -e "${YELLOW}版本号更新已完成，如需验证构建，请手动执行：${NC}"
echo -e "  编译检查: mvn compile -o                    # 离线编译检查"
echo -e "  完整构建: mvn clean compile -U             # 强制更新依赖后构建"
echo -e "  完整打包: mvn clean package                # 构建和打包"

# git状态检查
if git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo
    echo -e "${BLUE}=== Git 状态 ===${NC}"
    echo -e "${YELLOW}检测到git仓库，已修改的文件:${NC}"
    git status --short 2>/dev/null || echo "无法获取git状态"
    
    echo
    echo -e "${YELLOW}建议的后续操作:${NC}"
    echo -e "  1. 检查所有修改: git diff"
    echo -e "  2. 提交变更: git add . && git commit -m '升级版本到 $NEW_VERSION'"
    echo -e "  3. 创建标签: git tag v$NEW_VERSION"
fi

# 清理备份文件（可选）
echo
echo -e "${YELLOW}备份文件列表:${NC}"
ls -la pom.xml.backup.* 2>/dev/null || echo "未找到备份文件"
echo -e "${YELLOW}提示: 如需清理备份文件，请手动删除${NC}"

echo
echo -e "${GREEN}=== 版本号更新完成! ===${NC}"
echo -e "旧版本: $CURRENT_VERSION"
echo -e "新版本: $NEW_VERSION"
echo -e "${BLUE}请检查所有修改，然后提交到版本控制系统。${NC}"
