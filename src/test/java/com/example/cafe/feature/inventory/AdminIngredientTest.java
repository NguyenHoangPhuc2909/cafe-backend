package com.example.cafe.feature.inventory;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.feature.inventory.controller.AdminIngredientController;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.service.impl.AdminIngredientService;
import com.example.cafe.feature.inventory.service.impl.IngredientServiceImpl;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminIngredientTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private Fixture fixture;
  private static final String BODY = "{\"name\":\"  Cà phê hạt  \",\"unit\":\" g \",\"minThreshold\":500}";

  @BeforeEach void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class); context.refresh();
    fixture = context.getBean(Fixture.class);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }
  @AfterEach void close() { context.close(); }

  @Test void createReturns201AndNeverAcceptsClientStockOrSystemCode() throws Exception {
    mvc.perform(post("/api/admin/ingredients").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY.replace("}", ",\"stockQuantity\":999,\"code\":\"CUP_M\"}")))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("Cà phê hạt"))
        .andExpect(jsonPath("$.data.unit").value("g")).andExpect(jsonPath("$.data.stockQuantity").value(0))
        .andExpect(jsonPath("$.data.systemManaged").value(false));
    assertNull(fixture.item.getCode());
  }
  @Test void updatingMetadataPreservesStockAndSystemIdentity() throws Exception {
    fixture.item.setStockQuantity(1200.0); fixture.item.setCode("COFFEE"); fixture.referenced = true;
    mvc.perform(put("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY)).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stockQuantity").value(1200)).andExpect(jsonPath("$.data.unitEditable").value(false));
    assertEquals("COFFEE", fixture.item.getCode()); assertEquals(500.0, fixture.item.getMinThreshold());
  }
  @Test void unusedIngredientCanChangeUnitAndBeDeleted() throws Exception {
    mvc.perform(put("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY.replace(" g ", "kg")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.unit").value("kg"));
    mvc.perform(delete("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    assertTrue(fixture.deleted);
  }
  @Test void stockPreventsChangingUnitAndDeleting() throws Exception {
    fixture.item.setStockQuantity(2.0);
    assertProtected();
  }
  @Test void recipeOrMovementOrReservationHistoryPreventsChangingUnitAndDeleting() throws Exception {
    fixture.referenced = true;
    assertProtected();
  }
  private void assertProtected() throws Exception {
    mvc.perform(put("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY.replace(" g ", "kg"))).andExpect(status().isConflict());
    mvc.perform(delete("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
    assertEquals("g", fixture.item.getUnit()); assertFalse(fixture.deleted);
    assertFalse(fixture.ingredients.calls.containsKey("saveAndFlush"));
  }
  @Test void systemCupsAllowThresholdAndNameButNotUnitOrDeletion() throws Exception {
    fixture.item.setCode("CUP_M");
    mvc.perform(put("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY)).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.systemManaged").value(true)).andExpect(jsonPath("$.data.deletable").value(false));
    mvc.perform(put("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY.replace(" g ", "kg"))).andExpect(status().isConflict());
    mvc.perform(delete("/api/admin/ingredients/1").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
    assertEquals("CUP_M", fixture.item.getCode());
  }
  @Test void listExposesRestrictionsAndRecipeOptionsStillExcludeCups() throws Exception {
    fixture.item.setCode("CUP_L");
    mvc.perform(get("/api/admin/ingredients/management").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].unitEditable").value(false));
    mvc.perform(get("/api/admin/ingredients").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    fixture.item.setCode(null); fixture.referenced = true;
    mvc.perform(get("/api/admin/ingredients/management").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].deletable").value(false));
  }
  @Test void invalidNamesUnitsAndThresholdsReturn400() throws Exception {
    for (String body : List.of(BODY.replace("  Cà phê hạt  ", " "), BODY.replace(" g ", " "),
        BODY.replace("500", "-1"), BODY.replace("500", "null"), BODY.replace(" g ", "a".repeat(51)),
        BODY.replace("  Cà phê hạt  ", "a".repeat(256))))
      mvc.perform(post("/api/admin/ingredients").with(user("admin").roles("ADMIN"))
          .contentType("application/json").content(body)).andExpect(status().isBadRequest());
    assertFalse(fixture.ingredients.calls.containsKey("saveAndFlush"));
  }
  @Test void duplicateNamesAndMissingIngredientsAreRejected() throws Exception {
    fixture.duplicate = true;
    mvc.perform(post("/api/admin/ingredients").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY)).andExpect(status().isConflict());
    fixture.missing = true;
    mvc.perform(put("/api/admin/ingredients/999").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content(BODY)).andExpect(status().isNotFound());
    mvc.perform(delete("/api/admin/ingredients/999").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
  }
  @Test void onlyAdminMayManageIngredients() throws Exception {
    for (var request : List.of(get("/api/admin/ingredients/management"), post("/api/admin/ingredients"),
        put("/api/admin/ingredients/1"), delete("/api/admin/ingredients/1"))) {
      mvc.perform(request).andExpect(status().isUnauthorized());
      for (String role : List.of("STAFF", "CUSTOMER"))
        mvc.perform(request.with(user("other").roles(role))).andExpect(status().isForbidden());
    }
    assertTrue(fixture.ingredients.calls.isEmpty());
  }

  static class Fixture {
    final RepositoryStub<IngredientRepository> ingredients = new RepositoryStub<>(IngredientRepository.class);
    Ingredient item = new Ingredient(); boolean duplicate, referenced, missing, deleted;
    Fixture() {
      item.setId(1L); item.setName("Cà phê"); item.setUnit("g");
      ingredients.on("findAll", args -> List.of(item));
      ingredients.on("findReferencedIds", args -> referenced ? List.of(1L) : List.of());
      ingredients.on("isReferenced", args -> referenced);
      ingredients.on("findByIdForUpdate", args -> missing ? Optional.empty() : Optional.of(item));
      ingredients.on("existsByNameIgnoreCaseAndIdNot", args -> duplicate);
      ingredients.on("saveAndFlush", args -> { item = (Ingredient) args[0]; item.setId(1L); return item; });
      ingredients.on("delete", args -> { deleted = true; return null; });
      ingredients.on("flush", args -> null);
    }
  }
  @Configuration @EnableWebMvc static class TestConfig {
    @Bean JwtAuthenticationFilter filter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService users() { return name -> User.withUsername(name).password("unused").roles("STAFF").build(); }
    @Bean Fixture fixture() { return new Fixture(); }
    @Bean AdminIngredientController controller(Fixture fixture) {
      return new AdminIngredientController(new IngredientServiceImpl(fixture.ingredients.value), new AdminIngredientService(fixture.ingredients.value));
    }
    @Bean GlobalExceptionHandler errors() { return new GlobalExceptionHandler(); }
  }
}
