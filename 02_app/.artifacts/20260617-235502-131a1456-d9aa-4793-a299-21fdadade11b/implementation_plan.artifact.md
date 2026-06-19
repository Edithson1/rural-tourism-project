# Implementation Plan - Flow Fix and Currency Conversion

This plan addresses two main issues:
1.  **Flow Problem**: Reverting to "Independent" mode if the user aborts the device linking process (QR screen).
2.  **Currency Conversion**: Ensuring all prices (products, visits, dashboard) are displayed in the user's preferred currency (Soles, Dollars, Euros), regardless of how they were saved.

## User Review Required

> [!IMPORTANT]
> The database version will be incremented to add the `currency` field to the `Visit` entity. Existing data will be preserved as "S/" (Soles).

## Proposed Changes

### Sync and Flow Fix

#### [SyncViewModel.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/sync/SyncViewModel.kt)
- Add `cancelServerMode()` to reset the device to `CLIENT` role and restore the original `deviceId` if a connection wasn't established.
- Refactor `startServer` to be more robust.

#### [ShowQrScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/sync/ShowQrScreen.kt)
- Add a `DisposableEffect` to call `syncViewModel.cancelServerMode()` when the screen is disposed, unless `isConnected` is true.

---

### Currency Conversion Logic

#### [NEW] [CurrencyUtils.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/utils/CurrencyUtils.kt)
- Centralized conversion logic:
```kotlin
object CurrencyUtils {
    fun convert(amount: Double, from: String, to: String, usdRate: Double, eurRate: Double): Double {
        if (from == to) return amount
        val inSoles = when (from) {
            "S/" -> amount
            "$" -> amount * usdRate
            "€" -> amount * eurRate
            else -> amount
        }
        return when (to) {
            "S/" -> inSoles
            "$" -> inSoles / usdRate
            "€" -> inSoles / eurRate
            else -> inSoles
        }
    }
}
```

#### [Visit.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/data/local/Visit.kt)
- Add `val currency: String = "S/"` to the `Visit` entity to track the currency used at the time of sale.

#### [MainViewModel.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/MainViewModel.kt)
- Update `addVisit` to include the current `preferredCurrency` in the `Visit` object.

---

### UI Components Update

#### [ProductCatalogScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/profile/ProductCatalogScreen.kt)
- Use `CurrencyUtils.convert` to show all product prices in the preferred currency.
- Ensure sorting by price uses converted values.

#### [HomeScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/home/HomeScreen.kt)
- Convert `visit.totalAmount` in the "Recent Visits" card.

#### [DashboardViewModel.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/dashboard/DashboardViewModel.kt)
- Add a `StateFlow<AppSettings?>` to the ViewModel.
- Update all revenue and average calculations to convert each visit's `totalAmount` from its original currency to the current `preferredCurrency` using the stored exchange rates.

#### [VisitsScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/visits/VisitsScreen.kt) & [VisitDetailScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo sistemas web/rural-tourism-project/02_app/app/src/main/java/upch/mluque/final_project/ui/features/visits/VisitDetailScreen.kt)
- Update to display converted totals and product prices.

## Verification Plan

### Automated Tests
- No new automated tests are planned as the environment is mostly UI-driven.

### Manual Verification
1.  **Flow Fix**:
    - Open app -> Onboarding -> Options -> Link Device.
    - QR screen appears.
    - Press Back button.
    - Go to Profile. Verify it shows "Link Device" (CLIENT mode) and NOT "Logout" (SERVER mode).
2.  **Currency Conversion**:
    - Change preferred currency to USD in Profile.
    - Verify Product Catalog prices are shown in USD with converted values.
    - Add a visit.
    - Change preferred currency to Soles.
    - Verify the visit total in Home and Visits list is shown in Soles (converted from USD if saved in USD).
    - Verify Dashboard totals reflect the conversion.
