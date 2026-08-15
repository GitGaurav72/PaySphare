package com.PaySphere.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets Angular's client-side router handle direct visits such as
 * {@code /employees}. Files and API requests are handled by their normal
 * Spring MVC mappings before this fallback.
 */
@Controller
public class SpaWebController {

    @GetMapping({"/", "/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
