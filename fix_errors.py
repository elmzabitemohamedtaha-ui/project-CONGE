import re

def fix_file(file_path, replacements):
    with open(file_path, "r") as f:
        content = f.read()
    for old, new in replacements.items():
        content = content.replace(old, new)
    with open(file_path, "w") as f:
        f.write(content)

fix_file("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", {
    'title = com.example.ui.i18n.tr("dash_sync_title")': 'title = com.example.ui.i18n.I18nManager.getString("dash_sync_title")',
    'message = com.example.ui.i18n.tr("dash_sync")': 'message = com.example.ui.i18n.I18nManager.getString("dash_sync")',
    'snackbarHostState.showSnackbar(com.example.ui.i18n.tr("hist_empty"))': 'snackbarHostState.showSnackbar(com.example.ui.i18n.I18nManager.getString("hist_empty"))',
    'validationError = com.example.ui.i18n.tr("req_dates_error")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_error")',
    'validationError = com.example.ui.i18n.tr("req_dates_invalid")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_invalid")',
    'validationError = com.example.ui.i18n.tr("req_dates_past")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_past")',
    'validationError = com.example.ui.i18n.tr("req_dates_order")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_order")',
    'validationError = com.example.ui.i18n.tr("req_dates_equal")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_equal")',
    'validationError = com.example.ui.i18n.tr("req_balance_error")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_balance_error")'
})

fix_file("app/src/main/java/com/example/ui/components/LeaveRequestForm.kt", {
    'validationError = com.example.ui.i18n.tr("req_dates_error")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_error")',
    'validationError = com.example.ui.i18n.tr("req_dates_invalid")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_invalid")',
    'validationError = com.example.ui.i18n.tr("req_dates_past")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_past")',
    'validationError = com.example.ui.i18n.tr("req_dates_order")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_order")',
    'validationError = com.example.ui.i18n.tr("req_dates_equal")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_dates_equal")',
    'validationError = com.example.ui.i18n.tr("req_balance_error")': 'validationError = com.example.ui.i18n.I18nManager.getString("req_balance_error")',
    'submitSuccessMessage = com.example.ui.i18n.tr("req_success")': 'submitSuccessMessage = com.example.ui.i18n.I18nManager.getString("req_success")'
})

