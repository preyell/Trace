// src/main/java/com/sybyl/trace/web/IpUtils.java
package com.sybyl.trace.web;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return null;

        // 1) Try X-Forwarded-For if behind reverse proxy / load-balancer
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // XFF may contain multiple IPs: client, proxy1, proxy2...
            ip = ip.split(",")[0].trim();
        } else {
            // 2) Fallback to remote address
            ip = request.getRemoteAddr();
        }

        if (ip == null) {
            return null;
        }

        // 3) Normalize IPv6 loopback to IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        // 4) If it's an IPv6 mapped IPv4 (::ffff:192.168.1.10), extract the IPv4 part
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            String last = parts[parts.length - 1];
            if (last.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                return last;
            }
        }

        return ip;
    }
}
