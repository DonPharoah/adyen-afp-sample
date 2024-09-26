package com.adyen.service;

import com.adyen.Client;
import com.adyen.config.ApplicationProperty;
import com.adyen.enums.Environment;
import com.adyen.model.IndividualSignup;
import com.adyen.model.OrganisationSignup;
import com.adyen.model.SoleProprietorshipSignup;
import com.adyen.model.legalentitymanagement.*;
import com.adyen.service.legalentitymanagement.BusinessLinesApi;
import com.adyen.service.legalentitymanagement.HostedOnboardingApi;
import com.adyen.service.legalentitymanagement.LegalEntitiesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps the Adyen Legal Entity Management API: legalEntities, transferInstruments, hostedOnboarding, etc..
 * It requires the BalancePlatform API key
 *
 * https://docs.adyen.com/api-explorer/legalentity/
 */
@Service
public class LegalEntityManagementAPIService {

    private final Logger log = LoggerFactory.getLogger(LegalEntityManagementAPIService.class);

    private Client apiClient = null;

    @Autowired
    private ApplicationProperty applicationProperty;

    public LegalEntity get(String id) {
        LegalEntity legalEntity = null;

        try {

            legalEntity = getLegalEntitiesApi().getLegalEntity(id);
            log.info(legalEntity.toString());

        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException("Cannot get LegalEntity: " + e.getMessage());
        }

        return legalEntity;
    }

    public LegalEntity create(IndividualSignup individualSignup) {

        LegalEntity legalEntity = null;

        try {
            LegalEntityInfoRequiredType legalEntityInfoRequiredType = new LegalEntityInfoRequiredType();
            legalEntityInfoRequiredType
                    .type(LegalEntityInfoRequiredType.TypeEnum.INDIVIDUAL)
                    .individual(new Individual()
                            .name(new com.adyen.model.legalentitymanagement.Name()
                                    .firstName(individualSignup.getFirstName())
                                    .lastName(individualSignup.getLastName()))
                            .residentialAddress(
                                    new com.adyen.model.legalentitymanagement.Address()
                                            .country(individualSignup.getCountryCode())
                            ));

            legalEntity = getLegalEntitiesApi().createLegalEntity(legalEntityInfoRequiredType);

        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException("Cannot create LegalEntity: " + e.getMessage());
        }

        return legalEntity;
    }

    public LegalEntity create(SoleProprietorshipSignup soleProprietorshipSignup) {

        LegalEntity legalEntity = null;

        try {
            LegalEntityInfoRequiredType legalEntityInfoRequiredType = new LegalEntityInfoRequiredType();
            legalEntityInfoRequiredType
                    .type(LegalEntityInfoRequiredType.TypeEnum.INDIVIDUAL)
                    .individual(new Individual()
                            .name(new com.adyen.model.legalentitymanagement.Name()
                                    .firstName(soleProprietorshipSignup.getFirstName())
                                    .lastName(soleProprietorshipSignup.getLastName()))
                            .residentialAddress(
                                    new com.adyen.model.legalentitymanagement.Address()
                                            .country(soleProprietorshipSignup.getCountryCode())
                            ));

            legalEntity = getLegalEntitiesApi().createLegalEntity(legalEntityInfoRequiredType);

        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException("Cannot create LegalEntity: " + e.getMessage());
        }

        return legalEntity;
    }

    public LegalEntity create(OrganisationSignup organisationSignup) {

        LegalEntity legalEntity = null;

        try {
            LegalEntityInfoRequiredType legalEntityInfoRequiredType = new LegalEntityInfoRequiredType();
            legalEntityInfoRequiredType
                    .type(LegalEntityInfoRequiredType.TypeEnum.ORGANIZATION)
                    .organization(new Organization()
                            .legalName(organisationSignup.getLegalName())
                            .registeredAddress(
                                    new Address()
                                            .country(organisationSignup.getCountryCode())
                            ));

            legalEntity = getLegalEntitiesApi().createLegalEntity(legalEntityInfoRequiredType);

        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException("Cannot create LegalEntity: " + e.getMessage());
        }

        return legalEntity;
    }

    /**
     * Create BusinessLine
     * @param legalEntityId
     * @return
     */
    public BusinessLine createBusinessLine(String legalEntityId) {

        BusinessLine businessLine;

        try {
            BusinessLineInfo businessLineInfo = new BusinessLineInfo();
            businessLineInfo
                    .legalEntityId(legalEntityId)
                    .industryCode("722513") // 	'Limited-service restaurants', see https://docs.adyen.com/platforms/verification-requirements/reference-additional-products/#list-industry-codes
                    .salesChannels(List.of("eCommerce", "payByLink"))
                    .service(BusinessLineInfo.ServiceEnum.PAYMENTPROCESSING)
                    .webData(List.of(new WebData()
                            .webAddress("https://example.com")));

            businessLine = getBusinessLinesApi().createBusinessLine(businessLineInfo);
            log.info("BusinessLine created id:{}, legalEntityId:{}", businessLine.getId(), businessLine.getLegalEntityId());

        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException("Cannot create BusinessLine: " + e.getMessage());
        }

        return businessLine;
    }


    /**
     * Generate the Hosted Onboarding link for the Legal entity
     * @param legalEntityId
     * @param host Host where the application is running
     * @return
     */
    public Optional<OnboardingLink> getOnboardingLink(String legalEntityId, String host) {

        Optional<OnboardingLink> onboardingLink = Optional.empty();

        try {

            onboardingLink = Optional.of(getHostedOnboardingApi()
                    .getLinkToAdyenhostedOnboardingPage(legalEntityId, getOnboardingLinkConfiguration(host)));
            log.info(onboardingLink.toString());

        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return onboardingLink;
    }

    /**
     * retrieve the desired customisation for the Hosted Onboarding app
     * @param host Host where the application is running
     * @return
     */
    private OnboardingLinkInfo getOnboardingLinkConfiguration(String host) {
        OnboardingLinkInfo onboardingLinkInfo = new OnboardingLinkInfo();

        // control the page language
        onboardingLinkInfo.setLocale("en-US");
        // link to bring users back to the platform
        onboardingLinkInfo.setRedirectUrl(host + "/dashboard");
        // custom theme applied to the Hosted Onboarding app (must be created in the Customer Area)
        // when undefined the default theme will be used
        onboardingLinkInfo.setThemeId(getApplicationProperty().getHostedOnboardingThemeId());
        // additional settings
        onboardingLinkInfo.setSettings(new OnboardingLinkSettings()
                .changeLegalEntityType(false)
                .editPrefilledCountry(false));

        return onboardingLinkInfo;
    }

    // LegalEntitiesApi handler
    private LegalEntitiesApi getLegalEntitiesApi() {
        return new LegalEntitiesApi(getApiClient());
    }

    // BusinessLinesApi handler
    private BusinessLinesApi getBusinessLinesApi() {
        return new BusinessLinesApi(getApiClient());
    }

    // HostedOnboardingApi handler
    private HostedOnboardingApi getHostedOnboardingApi() {
        return new HostedOnboardingApi(getApiClient());
    }

    // create client to access the Legal Entity Management API
    private Client getApiClient() {
        if (apiClient == null) {
            // create once
            apiClient = new Client(
                    applicationProperty.getLemApiKey(),
                    Environment.TEST); // change to LIVE on prod
        }

        return apiClient;
    }

    public ApplicationProperty getApplicationProperty() {
        return applicationProperty;
    }

    public void setApplicationProperty(ApplicationProperty applicationProperty) {
        this.applicationProperty = applicationProperty;
    }

}
