import java.io.Serializable;

public class Address implements Serializable {
    private static final long serialVersionUID = 1L; // Wichtig für das spätere Speichern

    private String id;
    private String firstName;
    private String lastName;
    private String street;
    private String zipCode;
    private String city;
    private String email;

    public Address(String id, String firstName, String lastName, String street, String zipCode, String city, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.zipCode = zipCode;
        this.city = city;
        this.email = email;
    }

    // Getter und Setter
    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Für die Anzeige in Listen (z. B. in einer JList oder JComboBox)
    @Override
    public String toString() {
        return lastName + ", " + firstName + " (" + city + ")";
    }
}
