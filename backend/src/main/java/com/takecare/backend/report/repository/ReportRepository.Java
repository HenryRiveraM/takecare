package com.takecare.report.repository;

import com.takecare.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ReportRepositoy 
        extends JpaRepository<Report, Integer> {

}