package com.example.cafe.feature.table;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.table.controller.AdminTableController;
import com.example.cafe.feature.table.controller.CafeTableController;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.feature.table.service.impl.AdminTableServiceImpl;
import com.example.cafe.feature.table.service.impl.CafeTableServiceImpl;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminTableTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private Fixture fixture;

  @BeforeEach
  void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class);
    context.refresh();
    fixture = context.getBean(Fixture.class);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void close() {
    context.close();
  }

  @Test
  void createReturns201TrimsNameGeneratesQrCodeAndIgnoresClientQrOrStatus() throws Exception {
    mvc.perform(post("/api/admin/tables").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"name\":\"  Bàn 09 - Tầng 2  \",\"qrCode\":\"client-qr\",\"isActive\":false}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.name").value("Bàn 09 - Tầng 2"))
        .andExpect(jsonPath("$.data.isActive").value(true))
        .andExpect(jsonPath("$.data.deletable").value(true))
        .andExpect(jsonPath("$.data.canDeactivate").value(true));

    assertNotNull(fixture.table.getQrCode());
    assertNotEquals("client-qr", fixture.table.getQrCode());
    assertTrue(fixture.table.getIsActive());
  }

  @Test
  void renamePreservesQrCodeAndActiveStatus() throws Exception {
    fixture.table.setQrCode("stable-qr-uuid-01");
    fixture.table.setIsActive(false);

    mvc.perform(put("/api/admin/tables/1").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"name\":\"  Bàn VIP 01  \",\"qrCode\":\"tampered-qr\",\"isActive\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Bàn VIP 01"))
        .andExpect(jsonPath("$.data.qrCode").value("stable-qr-uuid-01"))
        .andExpect(jsonPath("$.data.isActive").value(false));

    assertEquals("stable-qr-uuid-01", fixture.table.getQrCode());
    assertFalse(fixture.table.getIsActive());
  }

  @Test
  void unusedTableCanBeDeactivatedReactivatedAndDeleted() throws Exception {
    mvc.perform(patch("/api/admin/tables/1/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"isActive\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isActive").value(false));

    mvc.perform(patch("/api/admin/tables/1/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"isActive\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isActive").value(true));

    mvc.perform(delete("/api/admin/tables/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
    assertTrue(fixture.deleted);
  }

  @Test
  void openSessionBlocksDeactivationAndDeletionButAllowsRenaming() throws Exception {
    fixture.hasOpenSession = true;
    fixture.hasHistory = true;

    mvc.perform(patch("/api/admin/tables/1/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"isActive\":false}"))
        .andExpect(status().isConflict());

    mvc.perform(delete("/api/admin/tables/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isConflict());

    assertTrue(fixture.table.getIsActive());
    assertFalse(fixture.deleted);
    assertFalse(fixture.tables.calls.containsKey("saveAndFlush"));

    mvc.perform(put("/api/admin/tables/1").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"name\":\"Bàn 01 mới\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Bàn 01 mới"))
        .andExpect(jsonPath("$.data.qrCode").value("qr-table-1"))
        .andExpect(jsonPath("$.data.hasOpenSession").value(true))
        .andExpect(jsonPath("$.data.canDeactivate").value(false))
        .andExpect(jsonPath("$.data.deletable").value(false));
  }

  @Test
  void tableWithClosedSessionHistoryCanBeDeactivatedButCannotBeDeleted() throws Exception {
    fixture.hasOpenSession = false;
    fixture.hasHistory = true;

    mvc.perform(delete("/api/admin/tables/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isConflict());
    assertFalse(fixture.deleted);

    mvc.perform(patch("/api/admin/tables/1/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json")
            .content("{\"isActive\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isActive").value(false))
        .andExpect(jsonPath("$.data.hasSessionHistory").value(true))
        .andExpect(jsonPath("$.data.canDeactivate").value(true))
        .andExpect(jsonPath("$.data.deletable").value(false));
  }

  @Test
  void listExposesSessionAndProtectionFlagsAccurately() throws Exception {
    fixture.hasOpenSession = true;
    fixture.hasHistory = true;

    mvc.perform(get("/api/admin/tables").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].qrCode").value("qr-table-1"))
        .andExpect(jsonPath("$.data[0].hasOpenSession").value(true))
        .andExpect(jsonPath("$.data[0].openSessionId").value(77))
        .andExpect(jsonPath("$.data[0].hasSessionHistory").value(true))
        .andExpect(jsonPath("$.data[0].canDeactivate").value(false))
        .andExpect(jsonPath("$.data[0].deletable").value(false));
  }

  @Test
  void invalidNamesNullStatusDuplicateNamesAndMissingTablesAreRejected() throws Exception {
    for (String body : List.of("{\"name\":\"   \"}", "{\"name\":\"" + "a".repeat(21) + "\"}")) {
      mvc.perform(post("/api/admin/tables").with(user("admin").roles("ADMIN"))
              .contentType("application/json").content(body))
          .andExpect(status().isBadRequest());
    }

    mvc.perform(patch("/api/admin/tables/1/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"isActive\":null}"))
        .andExpect(status().isBadRequest());

    fixture.duplicate = true;
    mvc.perform(post("/api/admin/tables").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"name\":\"Bàn 01\"}"))
        .andExpect(status().isConflict());

    fixture.duplicate = false;
    fixture.missing = true;
    mvc.perform(put("/api/admin/tables/999").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"name\":\"Bàn 99\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(patch("/api/admin/tables/999/status").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"isActive\":false}"))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/admin/tables/999").with(user("admin").roles("ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void onlyAdminMayManageOrCreateTablesWhileStaffCanReadStaffTables() throws Exception {
    for (var request : List.of(
        get("/api/admin/tables"),
        post("/api/admin/tables").contentType("application/json").content("{\"name\":\"Bàn 02\"}"),
        put("/api/admin/tables/1").contentType("application/json").content("{\"name\":\"Bàn 02\"}"),
        patch("/api/admin/tables/1/status").contentType("application/json").content("{\"isActive\":false}"),
        delete("/api/admin/tables/1"),
        post("/api/staff/tables").contentType("application/json").content("{\"name\":\"Bàn 02\"}")
    )) {
      mvc.perform(request).andExpect(status().isUnauthorized());
      for (String role : List.of("STAFF", "CUSTOMER")) {
        mvc.perform(request.with(user("other").roles(role))).andExpect(status().isForbidden());
      }
    }

    mvc.perform(get("/api/staff/tables").with(user("staff").roles("STAFF")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("Bàn 01"));
  }

  static class Fixture {
    final RepositoryStub<CafeTableRepository> tables = new RepositoryStub<>(CafeTableRepository.class);
    final RepositoryStub<TableSessionRepository> sessions = new RepositoryStub<>(TableSessionRepository.class);

    CafeTable table = new CafeTable();
    boolean duplicate;
    boolean missing;
    boolean hasOpenSession;
    boolean hasHistory;
    boolean deleted;

    Fixture() {
      table.setId(1L);
      table.setName("Bàn 01");
      table.setQrCode("qr-table-1");
      table.setIsActive(true);

      tables.on("findAll", args -> List.of(table));
      tables.on("findByIdForUpdate", args -> missing ? Optional.empty() : Optional.of(table));
      tables.on("existsByNameIgnoreCaseAndIdNot", args -> duplicate);
      tables.on("existsByName", args -> duplicate);
      tables.on("saveAndFlush", args -> {
        table = (CafeTable) args[0];
        if (table.getId() == null) table.setId(1L);
        return table;
      });
      tables.on("delete", args -> {
        deleted = true;
        return null;
      });
      tables.on("flush", args -> null);

      sessions.on("findDistinctTableIds", args -> hasHistory ? List.of(1L) : List.of());
      sessions.on("findByStatus", args -> {
        if (!hasOpenSession) return List.of();
        TableSession open = new TableSession();
        open.setId(77L);
        open.setTable(table);
        open.setStatus(TableSessionStatus.OPEN);
        return List.of(open);
      });
      sessions.on("findByTable_IdAndStatus", args -> {
        if (!hasOpenSession) return Optional.empty();
        TableSession open = new TableSession();
        open.setId(77L);
        open.setTable(table);
        open.setStatus(TableSessionStatus.OPEN);
        return Optional.of(open);
      });
      sessions.on("existsByTable_Id", args -> hasHistory || hasOpenSession);
    }
  }

  @Configuration
  @EnableWebMvc
  static class TestConfig {
    @Bean
    JwtAuthenticationFilter filter() {
      return new JwtAuthenticationFilter(null, null);
    }

    @Bean
    UserDetailsService users() {
      return name -> User.withUsername(name).password("unused").roles("STAFF").build();
    }

    @Bean
    Fixture fixture() {
      return new Fixture();
    }

    @Bean
    AdminTableController adminTableController(Fixture fixture) {
      return new AdminTableController(new AdminTableServiceImpl(fixture.tables.value, fixture.sessions.value));
    }

    @Bean
    CafeTableController cafeTableController(Fixture fixture) {
      return new CafeTableController(new CafeTableServiceImpl(fixture.tables.value));
    }

    @Bean
    GlobalExceptionHandler errors() {
      return new GlobalExceptionHandler();
    }
  }
}
