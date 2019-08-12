package com.diviso.graeshoppe.offer.web.rest;

import com.diviso.graeshoppe.offer.OfferApp;

import com.diviso.graeshoppe.offer.domain.Offer;
import com.diviso.graeshoppe.offer.repository.OfferRepository;
import com.diviso.graeshoppe.offer.repository.search.OfferSearchRepository;
import com.diviso.graeshoppe.offer.service.OfferService;
import com.diviso.graeshoppe.offer.service.dto.OfferDTO;
import com.diviso.graeshoppe.offer.service.mapper.OfferMapper;
import com.diviso.graeshoppe.offer.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;


import static com.diviso.graeshoppe.offer.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the OfferResource REST controller.
 *
 * @see OfferResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OfferApp.class)
public class OfferResourceIntTest {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_PROMO_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PROMO_CODE = "BBBBBBBBBB";

    private static final Long DEFAULT_USAGE_COUNT = 1L;
    private static final Long UPDATED_USAGE_COUNT = 2L;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferMapper offerMapper;

    @Autowired
    private OfferService offerService;

    /**
     * This repository is mocked in the com.diviso.graeshoppe.offer.repository.search test package.
     *
     * @see com.diviso.graeshoppe.offer.repository.search.OfferSearchRepositoryMockConfiguration
     */
    @Autowired
    private OfferSearchRepository mockOfferSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restOfferMockMvc;

    private Offer offer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final OfferResource offerResource = new OfferResource(offerService);
        this.restOfferMockMvc = MockMvcBuilders.standaloneSetup(offerResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Offer createEntity(EntityManager em) {
        Offer offer = new Offer()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .createdDate(DEFAULT_CREATED_DATE)
            .updatedDate(DEFAULT_UPDATED_DATE)
            .promoCode(DEFAULT_PROMO_CODE)
            .usageCount(DEFAULT_USAGE_COUNT);
        return offer;
    }

    @Before
    public void initTest() {
        offer = createEntity(em);
    }

    @Test
    @Transactional
    public void createOffer() throws Exception {
        int databaseSizeBeforeCreate = offerRepository.findAll().size();

        // Create the Offer
        OfferDTO offerDTO = offerMapper.toDto(offer);
        restOfferMockMvc.perform(post("/api/offers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(offerDTO)))
            .andExpect(status().isCreated());

        // Validate the Offer in the database
        List<Offer> offerList = offerRepository.findAll();
        assertThat(offerList).hasSize(databaseSizeBeforeCreate + 1);
        Offer testOffer = offerList.get(offerList.size() - 1);
        assertThat(testOffer.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testOffer.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testOffer.getCreatedDate()).isEqualTo(DEFAULT_CREATED_DATE);
        assertThat(testOffer.getUpdatedDate()).isEqualTo(DEFAULT_UPDATED_DATE);
        assertThat(testOffer.getPromoCode()).isEqualTo(DEFAULT_PROMO_CODE);
        assertThat(testOffer.getUsageCount()).isEqualTo(DEFAULT_USAGE_COUNT);

        // Validate the Offer in Elasticsearch
        verify(mockOfferSearchRepository, times(1)).save(testOffer);
    }

    @Test
    @Transactional
    public void createOfferWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = offerRepository.findAll().size();

        // Create the Offer with an existing ID
        offer.setId(1L);
        OfferDTO offerDTO = offerMapper.toDto(offer);

        // An entity with an existing ID cannot be created, so this API call must fail
        restOfferMockMvc.perform(post("/api/offers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(offerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Offer in the database
        List<Offer> offerList = offerRepository.findAll();
        assertThat(offerList).hasSize(databaseSizeBeforeCreate);

        // Validate the Offer in Elasticsearch
        verify(mockOfferSearchRepository, times(0)).save(offer);
    }

    @Test
    @Transactional
    public void getAllOffers() throws Exception {
        // Initialize the database
        offerRepository.saveAndFlush(offer);

        // Get all the offerList
        restOfferMockMvc.perform(get("/api/offers?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(offer.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].updatedDate").value(hasItem(DEFAULT_UPDATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].promoCode").value(hasItem(DEFAULT_PROMO_CODE.toString())))
            .andExpect(jsonPath("$.[*].usageCount").value(hasItem(DEFAULT_USAGE_COUNT.intValue())));
    }
    
    @Test
    @Transactional
    public void getOffer() throws Exception {
        // Initialize the database
        offerRepository.saveAndFlush(offer);

        // Get the offer
        restOfferMockMvc.perform(get("/api/offers/{id}", offer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(offer.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.updatedDate").value(DEFAULT_UPDATED_DATE.toString()))
            .andExpect(jsonPath("$.promoCode").value(DEFAULT_PROMO_CODE.toString()))
            .andExpect(jsonPath("$.usageCount").value(DEFAULT_USAGE_COUNT.intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingOffer() throws Exception {
        // Get the offer
        restOfferMockMvc.perform(get("/api/offers/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateOffer() throws Exception {
        // Initialize the database
        offerRepository.saveAndFlush(offer);

        int databaseSizeBeforeUpdate = offerRepository.findAll().size();

        // Update the offer
        Offer updatedOffer = offerRepository.findById(offer.getId()).get();
        // Disconnect from session so that the updates on updatedOffer are not directly saved in db
        em.detach(updatedOffer);
        updatedOffer
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .createdDate(UPDATED_CREATED_DATE)
            .updatedDate(UPDATED_UPDATED_DATE)
            .promoCode(UPDATED_PROMO_CODE)
            .usageCount(UPDATED_USAGE_COUNT);
        OfferDTO offerDTO = offerMapper.toDto(updatedOffer);

        restOfferMockMvc.perform(put("/api/offers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(offerDTO)))
            .andExpect(status().isOk());

        // Validate the Offer in the database
        List<Offer> offerList = offerRepository.findAll();
        assertThat(offerList).hasSize(databaseSizeBeforeUpdate);
        Offer testOffer = offerList.get(offerList.size() - 1);
        assertThat(testOffer.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testOffer.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testOffer.getCreatedDate()).isEqualTo(UPDATED_CREATED_DATE);
        assertThat(testOffer.getUpdatedDate()).isEqualTo(UPDATED_UPDATED_DATE);
        assertThat(testOffer.getPromoCode()).isEqualTo(UPDATED_PROMO_CODE);
        assertThat(testOffer.getUsageCount()).isEqualTo(UPDATED_USAGE_COUNT);

        // Validate the Offer in Elasticsearch
        verify(mockOfferSearchRepository, times(1)).save(testOffer);
    }

    @Test
    @Transactional
    public void updateNonExistingOffer() throws Exception {
        int databaseSizeBeforeUpdate = offerRepository.findAll().size();

        // Create the Offer
        OfferDTO offerDTO = offerMapper.toDto(offer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOfferMockMvc.perform(put("/api/offers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(offerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Offer in the database
        List<Offer> offerList = offerRepository.findAll();
        assertThat(offerList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Offer in Elasticsearch
        verify(mockOfferSearchRepository, times(0)).save(offer);
    }

    @Test
    @Transactional
    public void deleteOffer() throws Exception {
        // Initialize the database
        offerRepository.saveAndFlush(offer);

        int databaseSizeBeforeDelete = offerRepository.findAll().size();

        // Get the offer
        restOfferMockMvc.perform(delete("/api/offers/{id}", offer.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Offer> offerList = offerRepository.findAll();
        assertThat(offerList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Offer in Elasticsearch
        verify(mockOfferSearchRepository, times(1)).deleteById(offer.getId());
    }

    @Test
    @Transactional
    public void searchOffer() throws Exception {
        // Initialize the database
        offerRepository.saveAndFlush(offer);
        when(mockOfferSearchRepository.search(queryStringQuery("id:" + offer.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(offer), PageRequest.of(0, 1), 1));
        // Search the offer
        restOfferMockMvc.perform(get("/api/_search/offers?query=id:" + offer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(offer.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].updatedDate").value(hasItem(DEFAULT_UPDATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].promoCode").value(hasItem(DEFAULT_PROMO_CODE)))
            .andExpect(jsonPath("$.[*].usageCount").value(hasItem(DEFAULT_USAGE_COUNT.intValue())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Offer.class);
        Offer offer1 = new Offer();
        offer1.setId(1L);
        Offer offer2 = new Offer();
        offer2.setId(offer1.getId());
        assertThat(offer1).isEqualTo(offer2);
        offer2.setId(2L);
        assertThat(offer1).isNotEqualTo(offer2);
        offer1.setId(null);
        assertThat(offer1).isNotEqualTo(offer2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OfferDTO.class);
        OfferDTO offerDTO1 = new OfferDTO();
        offerDTO1.setId(1L);
        OfferDTO offerDTO2 = new OfferDTO();
        assertThat(offerDTO1).isNotEqualTo(offerDTO2);
        offerDTO2.setId(offerDTO1.getId());
        assertThat(offerDTO1).isEqualTo(offerDTO2);
        offerDTO2.setId(2L);
        assertThat(offerDTO1).isNotEqualTo(offerDTO2);
        offerDTO1.setId(null);
        assertThat(offerDTO1).isNotEqualTo(offerDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(offerMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(offerMapper.fromId(null)).isNull();
    }
}
