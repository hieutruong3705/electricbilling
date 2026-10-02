package com.phenikaa.electricbilling.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Ghi CSV UTF-8 có BOM (Excel đọc đúng tiếng Việt), xuống dòng CRLF. */
public final class CsvWriter {

	private CsvWriter() {
	}

	public static byte[] build(List<String> header, List<List<String>> rows) {
		StringBuilder sb = new StringBuilder();
		appendRow(sb, header);
		for (List<String> row : rows) {
			appendRow(sb, row);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
		out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
		return out.toByteArray();
	}

	private static void appendRow(StringBuilder sb, List<String> cells) {
		for (int i = 0; i < cells.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(escape(cells.get(i)));
		}
		sb.append("\r\n");
	}

	/** Bao ngoặc kép khi cần; vô hiệu hóa ô bắt đầu bằng = + - @ để tránh CSV/formula injection. */
	static String escape(String value) {
		String v = value == null ? "" : value;
		if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
			v = "'" + v;
		}
		if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
			return "\"" + v.replace("\"", "\"\"") + "\"";
		}
		return v;
	}
}
