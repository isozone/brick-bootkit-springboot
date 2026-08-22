/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Copyright 2012-2021 the original author or authors.
 * Copy from spring-boot-loader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.loader.jar;

/**
 * Callback visitor triggered by {@link CentralDirectoryParser}.
 *
 * @author Phillip Webb
 */
public interface CentralDirectoryVisitor {

    /**
     * visitStart
     * @param endRecord endRecord
     * @param centralDirectoryData centralDirectoryData
     */
    void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData);

    /**
     * visitFileHeader
     * @param fileHeader fileHeader
     * @param dataOffset dataOffset
     */
    void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset);

    /**
     * visitEnd
     */
    void visitEnd();

}

