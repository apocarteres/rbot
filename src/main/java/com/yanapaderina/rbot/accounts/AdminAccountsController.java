package com.yanapaderina.rbot.accounts;

import com.yanapaderina.rbot.access.Roles;
import io.github.apocarteres.platform.auth.Account;
import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.AuthRefused;
import io.github.apocarteres.platform.auth.CurrentAccount;
import io.github.apocarteres.platform.auth.NoProfile;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// MVP-01, REQ-AUTH-009
@RestController
@RequestMapping("/api/admin/accounts")
class AdminAccountsController {

  static final int PAGE_SIZE = 20;
  static final Set<String> GRANTABLE = Set.of(Roles.ADMIN, Roles.PSYCHOLOGIST);

  private final Accounts accounts;

  AdminAccountsController(Accounts accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  AccountPage list(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page) {
    int current = Math.max(page, 0);
    String part = query.trim();
    return new AccountPage(
      accounts.search(part, current * PAGE_SIZE, PAGE_SIZE).stream().map(AccountView::of).toList(),
      accounts.count(part),
      current,
      PAGE_SIZE);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AccountView create(@Valid @RequestBody NewAccount request) {
    if (!GRANTABLE.containsAll(request.roles())) {
      throw new AccountRefused(AccountRefused.ROLES, "Роли вне перечня " + GRANTABLE);
    }
    if (accounts.findByEmail(request.email()).isPresent()) {
      throw new AuthRefused(AuthRefused.EMAIL_TAKEN, "Почта занята");
    }
    return AccountView.of(accounts.create(request.email(), request.password(), request.roles(), true, new NoProfile()));
  }

  @PutMapping("/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void setPassword(@PathVariable UUID id, @Valid @RequestBody NewPassword request) {
    accounts.setPassword(existing(id).id(), request.password());
  }

  @PostMapping("/{id}/block")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void block(@PathVariable UUID id) {
    Account account = existing(id);
    if (CurrentAccount.id().filter(account.id()::equals).isPresent()) {
      throw new AccountRefused(AccountRefused.SELF, "Своя учётная запись не блокируется");
    }
    accounts.block(account.id());
  }

  @PostMapping("/{id}/unblock")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unblock(@PathVariable UUID id) {
    accounts.unblock(existing(id).id());
  }

  private Account existing(UUID id) {
    return accounts.find(id).orElseThrow(() -> new AccountRefused(AccountRefused.MISSING, "Учётной записи нет"));
  }
}
