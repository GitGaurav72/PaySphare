package com.PaySphere.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets Angular's client-side router handle direct visits. Explicit routes are
 * used so static JavaScript/CSS files and API endpoints keep their normal
 * Spring MVC mappings.
 */
@Controller
public class SpaWebController {

    @GetMapping({
            "/",
            "/login",
            "/dashboard",
            "/employees",
            "/employees/new",
            "/employees/{id}",
            "/employees/{id}/edit",
            "/hr-users",
            "/forbidden"
    })
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
