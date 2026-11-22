package com.resolveit.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Association between a service and a qualified technician (removing it never deletes the account). */
@Entity
@Table(name = "service_technicians")
public class ServiceTechnician {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ItService service;
    @Column(name = "technician_id", nullable = false, length = 20)
    private String technicianId;

    protected ServiceTechnician() { }

    public ServiceTechnician(ItService service, String technicianId) {
        this.service = service;
        this.technicianId = technicianId;
    }

    public Long getId() { return id; }
    public ItService getService() { return service; }
    public String getTechnicianId() { return technicianId; }
}
