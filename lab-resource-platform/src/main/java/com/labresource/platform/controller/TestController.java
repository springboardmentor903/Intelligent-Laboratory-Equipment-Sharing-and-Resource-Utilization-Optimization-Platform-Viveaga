
package com.labresource.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // Any authenticated user can access this API
    @GetMapping("/api/test/protected")
    public String protectedApi() {
        return "Success! You are authenticated using JWT.";
    }

    // Only users with the RESEARCHER_STUDENT role can access this API
    @GetMapping("/api/test/student")
    public String studentApi() {
        return "Success! Student API accessed.";
    }

    // Only users with the Lab_Technician role can access this API
    @GetMapping("/api/test/technician")
    public String technicianApi() {
        return "Success! Lab Technician API accessed.";
    }

    // Only users with the LAB_MANAGER role can access this API
    @GetMapping("/api/test/manager")
    public String managerApi() {
        return "Success! Lab Manager API accessed.";
    }

    // Only users with the DEPARTMENT_HEAD role can access this API
    @GetMapping("/api/test/department-head")
    public String departmentHeadApi() {
        return "Success! Department Head API accessed.";
    }

    // Only users with the INSTITUTION_ADMINISTRATOR role can access this API
    @GetMapping("/api/test/institution-admin")
    public String institutionAdminApi() {
        return "Success! Institution Administrator API accessed.";
    }

    // Only users with the SYSTEM_ADMINISTRATOR role can access this API
    @GetMapping("/api/test/system-admin")
    public String systemAdminApi() {
        return "Success! System Administrator API accessed.";
    }

}
