package com.phenikaa.electricbilling.web;

import java.time.Clock;
import java.time.YearMonth;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.ReportService;

import lombok.RequiredArgsConstructor;

/** ADMIN: báo cáo doanh thu, danh sách nợ, tiêu thụ theo hộ; xuất CSV (UC11). */
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

	private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

	private final ReportService reportService;
	private final Clock clock;

	/** Trang báo cáo: {@code type} = revenue | debts | consumption. */
	@GetMapping
	public String reports(@RequestParam(defaultValue = "revenue") String type,
			@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			@RequestParam(required = false) String period, @RequestParam(defaultValue = "false") boolean overdueOnly,
			Model model) {
		YearMonth now = YearMonth.now(clock);
		YearMonth fromMonth = orDefault(WebUtils.parseMonth(from), YearMonth.of(now.getYear(), 1));
		YearMonth toMonth = orDefault(WebUtils.parseMonth(to), now);
		YearMonth periodMonth = orDefault(WebUtils.parseMonth(period), now);

		model.addAttribute("type", type);
		model.addAttribute("from", fromMonth.toString());
		model.addAttribute("to", toMonth.toString());
		model.addAttribute("period", periodMonth.toString());
		model.addAttribute("overdueOnly", overdueOnly);

		switch (type) {
			case "debts" -> model.addAttribute("debts", reportService.debts(overdueOnly));
			case "consumption" -> model.addAttribute("consumption", reportService.consumption(periodMonth));
			default -> {
				model.addAttribute("type", "revenue");
				try {
					model.addAttribute("revenue", reportService.revenue(fromMonth, toMonth));
				} catch (BusinessException ex) {
					model.addAttribute("error", ex.getMessage());
				}
			}
		}
		return "admin/reports";
	}

	@GetMapping("/revenue.csv")
	public ResponseEntity<byte[]> revenueCsv(@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		YearMonth now = YearMonth.now(clock);
		YearMonth fromMonth = orDefault(WebUtils.parseMonth(from), YearMonth.of(now.getYear(), 1));
		YearMonth toMonth = orDefault(WebUtils.parseMonth(to), now);
		return csv("bao-cao-doanh-thu.csv", reportService.revenueCsv(reportService.revenue(fromMonth, toMonth)));
	}

	@GetMapping("/debts.csv")
	public ResponseEntity<byte[]> debtsCsv(@RequestParam(defaultValue = "false") boolean overdueOnly) {
		return csv("danh-sach-no.csv", reportService.debtsCsv(reportService.debts(overdueOnly)));
	}

	@GetMapping("/consumption.csv")
	public ResponseEntity<byte[]> consumptionCsv(@RequestParam(required = false) String period) {
		YearMonth periodMonth = orDefault(WebUtils.parseMonth(period), YearMonth.now(clock));
		return csv("tieu-thu-" + periodMonth + ".csv",
				reportService.consumptionCsv(reportService.consumption(periodMonth)));
	}

	private static ResponseEntity<byte[]> csv(String filename, byte[] content) {
		return ResponseEntity.ok()
				.contentType(CSV)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.body(content);
	}

	private static YearMonth orDefault(YearMonth value, YearMonth fallback) {
		return value != null ? value : fallback;
	}
}
