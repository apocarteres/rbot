package com.yanapaderina.rbot.accounts;

import java.util.List;

// MVP-01, REQ-CODE-DESIGN-005
record AccountPage(List<AccountView> items, long total, int page, int size) {
}
