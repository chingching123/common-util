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
package com.fpx.common.util.excel.entity;


import com.fpx.common.util.excel.handler.inter.IExcelVerifyHandler;

public class ImportParams extends ExcelBaseParams {
	/**
	 * 表格标题行数,默认0
	 */
	private int titleRows = 0;
	/**
	 * 表头行数,默认1
	 */
	private int headRows = 1;
	/**
	 * 字段真正值和列标题之间的距离 默认0
	 */
	private int startRows = 0;
	/**
	 * 主键设置,如何这个cell没有值,就跳过 或者认为这个是list的下面的值
	 */
	private int keyIndex = 0;
	/**
	 * 上传表格需要读取的sheet 数量,默认为1
	 */
	private int sheetNum = 1;
	/**
	 * 是否需要保存上传的Excel,默认为false
	 */
	private boolean needSave = false;
	/**
	 * 保存上传的Excel目录,默认是 如 TestEntity这个类保存路径就是
	 * upload/excelUpload/Test/yyyyMMddHHmss_***** 保存名称上传时间_五位随机数
	 */
	private String saveUrl = "upload/excelUpload";
	/**
	 * 校验处理接口
	 */
	private IExcelVerifyHandler verifyHanlder;
	/**
	 * 最后的无效行数
	 */
	private int lastOfInvalidRow = 0;

	public int getHeadRows() {
		return headRows;
	}

	public int getKeyIndex() {
		return keyIndex;
	}

	public String getSaveUrl() {
		return saveUrl;
	}

	public int getSheetNum() {
		return sheetNum;
	}

	public int getStartRows() {
		return startRows;
	}

	public int getTitleRows() {
		return titleRows;
	}

	public IExcelVerifyHandler getVerifyHanlder() {
		return verifyHanlder;
	}

	public boolean isNeedSave() {
		return needSave;
	}

	public void setHeadRows(int headRows) {
		this.headRows = headRows;
	}

	public void setKeyIndex(int keyIndex) {
		this.keyIndex = keyIndex;
	}

	public void setNeedSave(boolean needSave) {
		this.needSave = needSave;
	}

	public void setSaveUrl(String saveUrl) {
		this.saveUrl = saveUrl;
	}

	public void setSheetNum(int sheetNum) {
		this.sheetNum = sheetNum;
	}

	public void setStartRows(int startRows) {
		this.startRows = startRows;
	}

	public void setTitleRows(int titleRows) {
		this.titleRows = titleRows;
	}

	public void setVerifyHanlder(IExcelVerifyHandler verifyHanlder) {
		this.verifyHanlder = verifyHanlder;
	}

	public int getLastOfInvalidRow() {
		return lastOfInvalidRow;
	}

	public void setLastOfInvalidRow(int lastOfInvalidRow) {
		this.lastOfInvalidRow = lastOfInvalidRow;
	}

}
