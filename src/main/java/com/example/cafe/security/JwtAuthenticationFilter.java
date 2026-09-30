package com.example.cafe.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// Kế thừa OncePerRequestFilter để đảm bảo mỗi Request chỉ bị chặn lại xét hỏi đúng 1 lần
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Lột vé từ trên trán khách xuống (lấy Token từ Header)
            String jwt = getJwtFromRequest(request);

            // 2. Nếu khách có vé VÀ vé là hàng xịn
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                
                // 3. Đọc tên khách ghi trên vé
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 4. Lôi hồ sơ của khách từ DB lên (kiểm tra xem khách còn tồn tại không)
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // 5. Đóng mộc "ĐÃ KIỂM TRA", cấp quyền đi qua cổng
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Báo cho sếp tổng Spring Security biết: "Khách này VIP, vé chuẩn, cho qua!"
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            System.out.println("Lỗi tại trạm gác: " + ex.getMessage());
        }

        // Dù khách có vé hay không (hoặc vé giả) thì vẫn mở cổng cho đi tiếp.
        // Chú ý: Đi tiếp vào trong sẽ đụng thằng trùm cuối SecurityConfig, lúc đó không có vé thì nó mới đuổi (báo 401)
        filterChain.doFilter(request, response);
    }

    // Hàm phụ: Chuyên đi móc túi lấy Token từ Header
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // Kiểm tra xem có Header không, và có bắt đầu bằng chữ "Bearer " chuẩn quốc tế không
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Cắt bỏ 7 ký tự "Bearer " để lấy lõi Token
        }
        return null; // Trả về null nếu khách đi tay không
    }
}
