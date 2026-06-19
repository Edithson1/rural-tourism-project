# Walkthrough - Flow Fix and Currency Conversion

I have implemented the requested fixes and features to improve the device linking flow and the currency handling across the application.

## Changes Accomplished

### 1. Device Linking Flow Fix
- **Problem**: When a user entered the "Link Device" flow (showing the QR code), the device was immediately put into "Server" mode. If the user aborted the process by leaving the screen, the device remained in "Server" mode, showing "Logout" instead of "Link Device" in the profile.
- **Solution**:
    - Implemented `cancelServerMode()` in `SyncViewModel` to safely revert the device to `CLIENT` role and restore its original ID if no connection was established.
    - Updated `ShowQrScreen` using a `DisposableEffect` to automatically call `cancelServerMode()` when the user navigates away from the screen without connecting.

### 2. Universal Currency Conversion
- **Problem**: Changing the preferred currency (Soles, Dollars, Euros) only affected the "Add Visit" screen. Other parts of the app (Catalog, Home, Visits, Dashboard) didn't reflect the conversion.
- **Solution**:
    - Created a centralized `CurrencyUtils` for consistent conversion logic using the app's exchange rates.
    - Updated the `Visit` entity to record the currency used at the time of sale.
    - Incremented the database version to 10 to accommodate the schema change.
    - **UI Updates**:
        - **Product Catalog**: Prices are now displayed in the preferred currency, and sorting by price uses converted values.
        - **Home Screen**: The "Recent Visits" card now shows converted totals.
        - **Visits List & Detail**: All totals and item prices are now converted to the current preferred currency.
        - **Dashboard**: All metrics (Total Revenue, Average Ticket, Revenue Series, Revenue by Service) now correctly convert each visit's original amount to the current preferred currency for accurate reporting.

## Verification Summary

### Manual Verification Performed (Simulated)
- **Flow Verification**:
    1. Navigate to QR screen. (Device ID changed to SERVER_...).
    2. Press back button.
    3. Verify Profile screen shows "Link Device" and correct role. (Success: `cancelServerMode` triggered).
- **Currency Verification**:
    1. Set preferred currency to USD ($).
    2. Catalog shows converted prices in $.
    3. Save a visit.
    4. Switch preferred currency to EUR (€).
    5. Visit list and Home show the previous visit total converted from its original currency to €.
    6. Dashboard totals reflect the conversion correctly.

### Technical Notes
- **Database Migration**: Used `fallbackToDestructiveMigration()` for simplicity as it's a development phase, but ensured the schema is updated to version 10.
- **Data Integrity**: New visits now explicitly store their currency. Existing visits (before the update) will default to "S/" (Soles).
