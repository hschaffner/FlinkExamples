
package io.confluent.heinz.test;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * The root schema
 * <p>
 * The root schema comprises the entire JSON document.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "first_name",
    "last_name",
    "customer_id"
})
@Generated("jsonschema2pojo")
public class JsonMsg {

    /**
     * Client Firstname
     * <p>
     * A Customer's first name
     * (Required)
     * 
     */
    @JsonProperty("first_name")
    @JsonPropertyDescription("A Customer's first name")
    private String firstName = "";
    /**
     * Client Lastname
     * <p>
     * A Customer's last name
     * (Required)
     * 
     */
    @JsonProperty("last_name")
    @JsonPropertyDescription("A Customer's last name")
    private String lastName = "";
    /**
     * Customer ID
     * <p>
     * The indexed ID for a customer
     * (Required)
     * 
     */
    @JsonProperty("customer_id")
    @JsonPropertyDescription("The indexed ID for a customer")
    private Integer customerId;

    /**
     * Client Firstname
     * <p>
     * A Customer's first name
     * (Required)
     * 
     */
    @JsonProperty("first_name")
    public String getFirstName() {
        return firstName;
    }

    /**
     * Client Firstname
     * <p>
     * A Customer's first name
     * (Required)
     * 
     */
    @JsonProperty("first_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Client Lastname
     * <p>
     * A Customer's last name
     * (Required)
     * 
     */
    @JsonProperty("last_name")
    public String getLastName() {
        return lastName;
    }

    /**
     * Client Lastname
     * <p>
     * A Customer's last name
     * (Required)
     * 
     */
    @JsonProperty("last_name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Customer ID
     * <p>
     * The indexed ID for a customer
     * (Required)
     * 
     */
    @JsonProperty("customer_id")
    public Integer getCustomerId() {
        return customerId;
    }

    /**
     * Customer ID
     * <p>
     * The indexed ID for a customer
     * (Required)
     * 
     */
    @JsonProperty("customer_id")
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(JsonMsg.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("firstName");
        sb.append('=');
        sb.append(((this.firstName == null)?"<null>":this.firstName));
        sb.append(',');
        sb.append("lastName");
        sb.append('=');
        sb.append(((this.lastName == null)?"<null>":this.lastName));
        sb.append(',');
        sb.append("customerId");
        sb.append('=');
        sb.append(((this.customerId == null)?"<null>":this.customerId));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.customerId == null)? 0 :this.customerId.hashCode()));
        result = ((result* 31)+((this.firstName == null)? 0 :this.firstName.hashCode()));
        result = ((result* 31)+((this.lastName == null)? 0 :this.lastName.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof JsonMsg) == false) {
            return false;
        }
        JsonMsg rhs = ((JsonMsg) other);
        return ((((this.customerId == rhs.customerId)||((this.customerId!= null)&&this.customerId.equals(rhs.customerId)))&&((this.firstName == rhs.firstName)||((this.firstName!= null)&&this.firstName.equals(rhs.firstName))))&&((this.lastName == rhs.lastName)||((this.lastName!= null)&&this.lastName.equals(rhs.lastName))));
    }

}
