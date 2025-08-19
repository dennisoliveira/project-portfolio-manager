package com.github.dennisoliveira.portfolio.controller;

import com.github.dennisoliveira.portfolio.dto.PortfolioReportResponse;
import com.github.dennisoliveira.portfolio.service.PortfolioReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio")
public class PortfolioController {

    private final PortfolioReportService reportService;

    @Operation(summary = "Gerar relatório de projetos")
    @GetMapping("/report")
    public PortfolioReportResponse report() {
        return reportService.build();
    }
}