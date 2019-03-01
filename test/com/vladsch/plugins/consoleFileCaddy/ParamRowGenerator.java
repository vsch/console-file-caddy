/*
 * Copyright (c) 2015-2019 Vladimir Schneider <vladimir.schneider@gmail.com>, all rights reserved.
 *
 * This code is private property of the copyright holder and cannot be used without
 * having obtained a license or prior written permission of the of the copyright holder.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.vladsch.plugins.consoleFileCaddy;

import java.util.ArrayList;

class ParamRowGenerator extends ParameterizedRow {
    ArrayList<Object[]> rows = new ArrayList<>();

    /**
     * Create a param row generator for accumulating test paramters prefixed with file location  
     * 
     * @param locationPrefix  what to prefix the caller's class file name to arrive at an absolute 
     *                        file path of the source file.
     */
    public ParamRowGenerator(final String locationPrefix) {
        super(locationPrefix);
    }

    /**
     * Add parametrized test row and prefix with file location information at [0]
     * @param row an array of objects parameters for the test
     *            
     * @return this
     */
    public ParamRowGenerator row(Object[] row) {
        Object[] newRow = new Object[row.length + 1];
        System.arraycopy(row, 0, newRow, 1, row.length);
        
        // stack trace elements [1] is getCallerInfo function itself, [2] is this function, [3] is direct caller of this function, 
        // add 1 for every level of function call nesting before reaching here   
        LineInfo callerInfo = getCallerInfo(3);  
        
        int fileLine = callerInfo.line;
        String file = callerInfo.file;
        int index = callerInfo.index;
        
        newRow[0] = String.format("%d: %s:%d", index, file, fileLine);
        rows.add(newRow);
        
        return this;
    }
}
