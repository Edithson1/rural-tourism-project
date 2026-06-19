# Tasks

- [x] Fix Device Linking Flow (Independent vs Linked)
    - [x] Implement `cancelServerMode` in `SyncViewModel`
    - [x] Update `ShowQrScreen` to call `cancelServerMode` on exit
- [x] Implement Currency Conversion Logic
    - [x] Create `CurrencyUtils`
    - [x] Update `Visit` entity with `currency` field
    - [x] Update `MainViewModel.addVisit` to save current currency
- [x] Update UI for Currency Conversion
    - [x] Update `ProductCatalogScreen` display and sorting
    - [x] Update `HomeScreen` recent visits display
    - [x] Update `VisitsScreen` and `VisitItem` display
    - [x] Update `VisitDetailScreen` display
    - [x] Update `DashboardViewModel` to handle currency conversion for all metrics
- [/] Verification
    - [x] Verify no syntax errors in modified files
    - [ ] Verify flow fix (QR screen exit) - Manual
    - [ ] Verify currency display in all screens - Manual
    - [ ] Verify Dashboard calculations with multiple currencies - Manual
