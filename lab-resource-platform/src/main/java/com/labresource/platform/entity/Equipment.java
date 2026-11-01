
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "equipment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_equipment_asset_tag",
                        columnNames = "asset_tag"
                )
        }
)
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String model;

    private String manufacturer;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "asset_tag", nullable = false, unique = true)
    private String assetTag;

    private String location;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "hourly_cost", precision = 12, scale = 2)
    private BigDecimal hourlyCost;

    // Maximum operating hours available per day
    @Column(name = "available_hours_per_day", nullable = false)
    private BigDecimal availableHoursPerDay = BigDecimal.valueOf(8);

    // Equipment access mode: shared or exclusive
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_mode", nullable = false, length = 20)
    private UsageMode usageMode = UsageMode.SHARED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EquipmentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToMany
    @JoinTable(
            name = "equipment_tags",
            joinColumns = @JoinColumn(name = "equipment_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Specification> specifications = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<CalibrationRecord> calibrationRecords = new ArrayList<>();

    public Equipment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getHourlyCost() {
        return hourlyCost;
    }

    public void setHourlyCost(BigDecimal hourlyCost) {
        this.hourlyCost = hourlyCost;
    }

    public BigDecimal getAvailableHoursPerDay() {
        return availableHoursPerDay;
    }

    public void setAvailableHoursPerDay(BigDecimal availableHoursPerDay) {
        if (availableHoursPerDay == null
                || availableHoursPerDay.compareTo(BigDecimal.ZERO) <= 0
                || availableHoursPerDay.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new IllegalArgumentException(
                    "Available hours per day must be greater than 0 and at most 24"
            );
        }

        this.availableHoursPerDay = availableHoursPerDay;
    }

    public UsageMode getUsageMode() {
        return usageMode;
    }

    public void setUsageMode(UsageMode usageMode) {
        if (usageMode == null) {
            throw new IllegalArgumentException(
                    "Usage mode cannot be null"
            );
        }

        this.usageMode = usageMode;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public List<Specification> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<Specification> specifications) {
        this.specifications = specifications;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public List<CalibrationRecord> getCalibrationRecords() {
        return calibrationRecords;
    }

    public void setCalibrationRecords(
            List<CalibrationRecord> calibrationRecords) {
        this.calibrationRecords = calibrationRecords;
    }
}
