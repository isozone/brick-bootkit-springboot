/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.utils;



import com.zqzqq.bootkits.common.ManifestKey;

import com.zqzqq.bootkits.common.PackageStructure;

import com.zqzqq.bootkits.common.PackageType;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;



import java.io.*;

import java.math.BigInteger;

import java.nio.MappedByteBuffer;

import java.nio.channels.FileChannel;

import java.nio.charset.Charset;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.security.MessageDigest;

import java.util.Enumeration;

import java.util.List;

import java.util.Objects;

import java.util.jar.Attributes;

import java.util.jar.Manifest;

import java.util.zip.ZipEntry;

import java.util.zip.ZipFile;

/**
 * 插件文件工具类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public final class PluginFileUtils {

    private static final String FILE_POINT = ".";
    private static final Logger log = LoggerFactory.getLogger(PluginFileUtils.class);

    private PluginFileUtils(){}


    public static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return value;
    }


    public static void cleanEmptyFile(List<String> paths){
        if(ObjectUtils.isEmpty(paths)){
            return;
        }
        for (String pathStr : paths) {
            Path path = Paths.get(pathStr);
            if(!Files.exists(path)){
                continue;
            }
            try {
                Files.list(path)
                        .forEach(subPath -> {
                            File file = subPath.toFile();
                            if(!file.isFile()){
                                return;
                            }
                            long length = file.length();
                            if(length == 0){
                                try {
                                    Files.deleteIfExists(subPath);
                                } catch (IOException e) {
                                    log.error("Failed to delete empty file: {}", subPath, e);
                                }
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to list files in path: {}", path, e);
            }
        }
    }


    /**
     * 如果文件不存在，则会创建
     * @param path 插件路径
     * @return 插件路径
     * @throws IOException 没有发现文件异常
     */
    public static File createExistFile(Path path) throws IOException {
        Path parent = path.getParent();
        if(!Files.exists(parent)){
            Files.createDirectories(parent);
        }
        if(!Files.exists(path)){
            Files.createFile(path);
        }
        return path.toFile();
    }

    /**
     * 得到文件名称
     * @param file 原始文件
     * @param includeSuffix 是否包含后缀
     * @return String
     */
    public static String getFileName(File file, boolean includeSuffix){
        if(file == null){
            return null;
        }
        return getFileName(file.getName(), includeSuffix);
    }


    /**
     * 获取不包含文件后缀的文件名称
     * @param fileName 文件名称
     * @return String
     */
    public static String getFileName(String fileName){
        return getFileName(fileName, false);
    }

    /**
     * 得到文件名称
     * @param fileName 原始文件名称. 例如: file.txt , 则返回 file
     * @param includeSuffix 是否包含后缀
     * @return String
     */
    public static String getFileName(String fileName, boolean includeSuffix){
        if(ObjectUtils.isEmpty(fileName)){
            return fileName;
        }
        if(includeSuffix){
            return fileName;
        }
        int i = fileName.lastIndexOf(FILE_POINT);
        if(i > 0){
            return fileName.substring(0, i);
        } else {
            return fileName;
        }
    }

    /**
     * 创建随机文件
     * @param parentPath 父目录
     * @param isFile 是否为文件，不为文件就创建目录
     * @param fileSuffix 文件后缀
     * @return 创建的File
     * @throws Exception 创建异常
     */
    public static File createRandomFile(String parentPath, boolean isFile, String fileSuffix) throws Exception{
        if(isFile){
            if(ObjectUtils.isEmpty(fileSuffix)){
                fileSuffix = "";
            } else {
                if(!fileSuffix.startsWith(".")){
                    fileSuffix = "." + fileSuffix;
                }
            }
            File file = new File(parentPath, String.valueOf(System.currentTimeMillis()) + fileSuffix);
            if(file.createNewFile()){
                return file;
            } else {
                throw new IOException("Cannot create file '" + file + "'.");
            }
        } else {
            File file = new File(parentPath, String.valueOf(System.currentTimeMillis()));
            FileUtils.forceMkdir(file);
            return file;
        }
    }

    /**
     * 判断是否能解压
     * @param zipPath zip路径
     * @return boolean
     */
    public static boolean canDecompressZip(String zipPath){
        return ResourceUtils.isZip(zipPath) || ResourceUtils.isJar(zipPath);
    }

    /**
     * 解压zip 文件
     * @param zipPath zip 文件
     * @param targetDir 目标目录
     * @return 解压后的目录
     * @throws IOException 解压异常
     */
    public static File decompressZip(String zipPath, String targetDir) throws IOException {
        File zipFile = new File(zipPath);
        if(!canDecompressZip(zipPath)){
            throw new IOException("文件[" + zipFile.getName() + "]非压缩包, 不能解压");
        }
        File targetDirFile = new File(targetDir, getFileName(zipFile, false));
        if(!targetDirFile.exists()){
            if(!targetDirFile.mkdirs()){
                throw new IOException("创建目录异常: " + targetDir);
            }
            targetDir = targetDirFile.getAbsolutePath();
        } else {
            // 存在相同目录
            targetDirFile = new File(targetDir, getFileName(zipFile, false)
                    + "_" + System.currentTimeMillis());
            FileUtils.forceMkdir(targetDirFile);
            targetDir = targetDirFile.getAbsolutePath();
        }
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName(PackageStructure.CHARSET_NAME))) {
            Enumeration<? extends ZipEntry> zipEnumeration = zip.entries();
            ZipEntry zipEntry = null;
            while (zipEnumeration.hasMoreElements()) {
                zipEntry = zipEnumeration.nextElement();
                String zipEntryName = zipEntry.getName();
                String currentZipPath = PackageStructure.resolvePath(zipEntryName);
                String currentTargetPath = FilesUtils.joiningFilePath(targetDir, currentZipPath);
                //判断路径是否存在,不存在则创建文件路径
                if (zipEntry.isDirectory()) {
                    File file = new File(currentTargetPath);
                    FileUtils.forceMkdir(file);
                    continue;
                } else {
                    FilesUtils.createFile(currentTargetPath);
                }
                try (InputStream in = zip.getInputStream(zipEntry);
                     FileOutputStream out = new FileOutputStream(currentTargetPath)){
                    if(PackageStructure.PROD_MANIFEST_PATH.equals(zipEntryName)){
                        // 如果是Manifest 文件, 则将打包类型切换为xx-outer
                        resolveDecompressPluginType(in, out);
                    } else {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
        return targetDirFile;
    }

    private static void resolveDecompressPluginType(InputStream inputStream, OutputStream outputStream) throws IOException{
        Manifest manifest = new Manifest(inputStream);
        Attributes mainAttributes = manifest.getMainAttributes();
        String value = mainAttributes.getValue(ManifestKey.PLUGIN_PACKAGE_TYPE);
        if(Objects.equals(value, PackageType.MAIN_PACKAGE_TYPE_JAR)){
            value = PackageType.PLUGIN_PACKAGE_TYPE_DIR;
        } else if(Objects.equals(value, PackageType.PLUGIN_PACKAGE_TYPE_ZIP)){
            value = PackageType.PLUGIN_PACKAGE_TYPE_DIR;
        }
        mainAttributes.putValue(ManifestKey.PLUGIN_PACKAGE_TYPE, value);
        manifest.write(outputStream);
    }

}

