/**
 * Copyright 2013-2015 JueYue (qrb.jueyue@gmail.com)
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fpx.common.util.excel.word.parse.excel;

import java.util.List;

import com.fpx.common.util.excel.util.PoiPublicUtil;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * 处理和生成Map 类型的数据变成表格
 * 
 * @author JueYue
 * @date 2014年8月9日 下午10:28:46
 */
public final class ExcelMapParse {

	/**
	 * 解析参数行,获取参数列表
	 * 
	 * @Author JueYue
	 * @date 2013-11-18
	 * @param currentRow
	 * @return
	 */
	private static String[] parseCurrentRowGetParams(XWPFTableRow currentRow) {
		List<XWPFTableCell> cells = currentRow.getTableCells();
		String[] params = new String[cells.size()];
		String text;
		for (int i = 0; i < cells.size(); i++) {
			text = cells.get(i).getText();
			params[i] = text == null ? "" : text.trim().replace("{{", "").replace("}}", "");
		}
		return params;
	}

	/**
	 * 解析下一行,并且生成更多的行
	 * 
	 * @Author JueYue
	 * @date 2013-11-18
	 * @param table
	 * @param listobj2
	 */
	public static void parseNextRowAndAddRow(XWPFTable table, int index, List<Object> list) throws Exception {
		XWPFTableRow currentRow = table.getRow(index);
		String[] params = parseCurrentRowGetParams(currentRow);
		table.removeRow(index);// 移除这一行
		int cellIndex = 0;// 创建完成对象一行好像多了一个cell
		for (Object obj : list) {
			currentRow = table.createRow();
			for (cellIndex = 0; cellIndex < currentRow.getTableCells().size(); cellIndex++) {
				currentRow.getTableCells().get(cellIndex).setText(PoiPublicUtil.getValueDoWhile(obj, params[cellIndex].split("\\."), 0).toString());
			}
			for (; cellIndex < params.length; cellIndex++) {
				currentRow.createCell().setText(PoiPublicUtil.getValueDoWhile(obj, params[cellIndex].split("\\."), 0).toString());
			}
		}

	}

}
