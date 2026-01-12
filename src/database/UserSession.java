package database;

public class UserSession {
    private final String login;
    private final String role;
    private final Integer cashierNumber;
    private final Integer cashdeskNumber;
    private Long shiftNumber;

    public UserSession(String login, String role, Integer cashierNumber, Integer cashdeskNumber) {
        this.login = login;
        this.role = role;
        this.cashierNumber = cashierNumber;
        this.cashdeskNumber = cashdeskNumber;
    }

    public String getLogin() {
        return login;
    }

    public String getRole() {
        return role;
    }

    public Integer getCashierNumber() {
        return cashierNumber;
    }

    public Integer getCashdeskNumber() {
        return cashdeskNumber;
    }

    public Long getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Long shiftNumber) {
        this.shiftNumber = shiftNumber;
    }
}
