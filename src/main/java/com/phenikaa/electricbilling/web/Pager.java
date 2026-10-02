package com.phenikaa.electricbilling.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/** Sinh liên kết phân trang giữ nguyên các tham số lọc hiện tại. Gọi trong template: {@code @pager}. */
@Component("pager")
public class Pager {

	/** URL của trang {@code page} (đánh số từ 0) với mọi tham số hiện tại, trừ {@code page}. */
	public String url(int page) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		StringBuilder sb = new StringBuilder(request.getRequestURI()).append('?');
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			if (e.getKey().equals("page")) {
				continue;
			}
			for (String v : e.getValue()) {
				if (v != null && !v.isEmpty()) {
					sb.append(encode(e.getKey())).append('=').append(encode(v)).append('&');
				}
			}
		}
		return sb.append("page=").append(page).toString();
	}

	/** Các số trang hiển thị: cửa sổ ±2 quanh trang hiện tại. */
	public List<Integer> window(Page<?> page) {
		List<Integer> pages = new ArrayList<>();
		int from = Math.max(0, page.getNumber() - 2);
		int to = Math.min(page.getTotalPages() - 1, page.getNumber() + 2);
		for (int i = from; i <= to; i++) {
			pages.add(i);
		}
		return pages;
	}

	private static String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
