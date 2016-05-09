package com.taojiaen.security;
import java.util.HashMap;

import javax.annotation.Resource;

import org.broadleafcommerce.common.security.util.PasswordChange;
import org.broadleafcommerce.common.util.TransactionUtils;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerServiceImpl;
import org.broadleafcommerce.profile.core.service.handler.PasswordUpdatedHandler;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;


public class MDCustomerServiceImpl extends CustomerServiceImpl{
	 @Resource(name = "mdPasswordEncoder")
	private MessageDigestPasswordEncoder encoder;
    @Override
    public Customer registerCustomer(Customer customer, String password, String passwordConfirm) {
       password =  encoder.encodePassword(password, customer.getUsername());
       passwordConfirm = encoder.encodePassword(passwordConfirm, customer.getUsername());
       customer.setRegistered(true);

       // When unencodedPassword is set the save() will encode it
       if (customer.getId() == null) {
           customer.setId(findNextCustomerId());
       }
       customer.setUnencodedPassword(password);
       Customer retCustomer = saveCustomer(customer);
       createRegisteredCustomerRoles(retCustomer);
       
       HashMap<String, Object> vars = new HashMap<String, Object>();
       vars.put("customer", retCustomer);
       /**
        * 不再发送email
        */
     //  emailService.sendTemplateEmail(customer.getEmailAddress(), getRegistrationEmailInfo(), vars);        
       notifyPostRegisterListeners(retCustomer);
       return retCustomer;
    }
    @Override
    @Transactional(TransactionUtils.DEFAULT_TRANSACTION_MANAGER)
    public Customer changePassword(PasswordChange passwordChange) {
        Customer customer = readCustomerByUsername(passwordChange.getUsername());
        customer.setUnencodedPassword(encoder.encodePassword(passwordChange.getNewPassword(),
        		customer.getUsername()));
        customer.setPasswordChangeRequired(passwordChange.getPasswordChangeRequired());
        customer = saveCustomer(customer);
        
        for (PasswordUpdatedHandler handler : passwordChangedHandlers) {
            handler.passwordChanged(passwordChange, customer, passwordChange.getNewPassword());
        }
        
        return customer;
    }
}
