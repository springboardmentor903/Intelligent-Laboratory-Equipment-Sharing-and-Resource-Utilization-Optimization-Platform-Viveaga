
package com.labresource.platform.controller;

import com.labresource.platform.dto.EquipmentUtilizationDTO;
import com.labresource.platform.dto.DepartmentUtilizationDTO;
import com.labresource.platform.dto.InstitutionUtilizationDTO;
import com.labresource.platform.dto.UsageModeUtilizationDTO;
import com.labresource.platform.service.UtilizationService;
import com.labresource.platform.dto.HistoricalBenchmarkDTO;
import com.labresource.platform.dto.IdleTimeDTO;
import com.labresource.platform.dto.PeakUsageDTO;
import com.labresource.platform.dto.EquipmentHeatmapDTO;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.labresource.platform.dto.DemandAnalysisDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics/utilization")
public class UtilizationController {

    private final UtilizationService utilizationService;

    public UtilizationController(
            UtilizationService utilizationService) {
        this.utilizationService = utilizationService;
    }

    @GetMapping("/equipment")
    public List<EquipmentUtilizationDTO> getEquipmentUtilization(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateEquipmentUtilization(
                startDate,
                endDate
        );
    }

    // Department-wise utilization API
    @GetMapping("/department")
    public List<DepartmentUtilizationDTO> getDepartmentUtilization(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateDepartmentUtilization(
                startDate,
                endDate
        );
    }

    // Institution-wise utilization API
    @GetMapping("/institution")
    public List<InstitutionUtilizationDTO> getInstitutionUtilization(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateInstitutionUtilization(
                startDate,
                endDate
        );
    }

    // Shared vs Exclusive utilization API
    @GetMapping("/usage-mode")
    public List<UsageModeUtilizationDTO> getUsageModeUtilization(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateUsageModeUtilization(
                startDate,
                endDate
        );
    }

    // Equipment-wise demand analysis API
    @GetMapping("/demand")
    public List<DemandAnalysisDTO> getDemandAnalysis(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateDemandAnalysis(
                startDate,
                endDate
        );
    }

    // Historical utilization benchmark API
    @GetMapping("/historical-benchmark")
    public List<HistoricalBenchmarkDTO> getHistoricalBenchmark(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateHistoricalBenchmark(
                startDate,
                endDate
        );
    }

    // Equipment idle-time detection API
    @GetMapping("/idle-time")
    public List<IdleTimeDTO> getIdleTime() {

        return utilizationService.calculateIdleTime();
    }

    // Hour-wise peak usage API
    @GetMapping("/peak-usage/hourly")
    public List<PeakUsageDTO> getHourlyPeakUsage(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateHourlyPeakUsage(
                startDate,
                endDate
        );
    }


    // Day-wise peak usage API
    @GetMapping("/peak-usage/daily")
    public List<PeakUsageDTO> getDailyPeakUsage(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return utilizationService.calculateDailyPeakUsage(
                startDate,
                endDate
        );
    }

    // Equipment-wise hourly usage heatmap API
    @GetMapping("/heatmap")
    public List<EquipmentHeatmapDTO> getEquipmentHeatmap(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return utilizationService.calculateEquipmentHeatmap(
                startDate,
                endDate
        );
    }
}
