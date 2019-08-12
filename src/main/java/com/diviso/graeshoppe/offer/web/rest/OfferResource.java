package com.diviso.graeshoppe.offer.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.diviso.graeshoppe.offer.model.OrderModel;
import com.diviso.graeshoppe.offer.service.OfferService;
import com.diviso.graeshoppe.offer.web.rest.errors.BadRequestAlertException;
import com.diviso.graeshoppe.offer.web.rest.util.HeaderUtil;
import com.diviso.graeshoppe.offer.web.rest.util.PaginationUtil;
import com.diviso.graeshoppe.offer.service.dto.OfferDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Offer.
 */
//@RestController
//@RequestMapping("/api")
public class OfferResource {

    private final Logger log = LoggerFactory.getLogger(OfferResource.class);

    private static final String ENTITY_NAME = "offerOffer";

    private final OfferService offerService;

    public OfferResource(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * POST  /offers : Create a new offer.
     *
     * @param offerDTO the offerDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new offerDTO, or with status 400 (Bad Request) if the offer has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/offers")
    @Timed
    public ResponseEntity<OfferDTO> createOffer(@RequestBody OfferDTO offerDTO) throws URISyntaxException {
        log.debug("REST request to save Offer : {}", offerDTO);
        if (offerDTO.getId() != null) {
            throw new BadRequestAlertException("A new offer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        OfferDTO result = offerService.save(offerDTO);
        return ResponseEntity.created(new URI("/api/offers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /offers : Updates an existing offer.
     *
     * @param offerDTO the offerDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated offerDTO,
     * or with status 400 (Bad Request) if the offerDTO is not valid,
     * or with status 500 (Internal Server Error) if the offerDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/offers")
    @Timed
    public ResponseEntity<OfferDTO> updateOffer(@RequestBody OfferDTO offerDTO) throws URISyntaxException {
        log.debug("REST request to update Offer : {}", offerDTO);
        if (offerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        OfferDTO result = offerService.save(offerDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, offerDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /offers : get all the offers.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of offers in body
     */
    @GetMapping("/offers")
    @Timed
    public ResponseEntity<List<OfferDTO>> getAllOffers(Pageable pageable) {
        log.debug("REST request to get a page of Offers");
        Page<OfferDTO> page = offerService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/offers");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /offers/:id : get the "id" offer.
     *
     * @param id the id of the offerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the offerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/offers/{id}")
    @Timed
    public ResponseEntity<OfferDTO> getOffer(@PathVariable Long id) {
        log.debug("REST request to get Offer : {}", id);
        Optional<OfferDTO> offerDTO = offerService.findOne(id);
        return ResponseUtil.wrapOrNotFound(offerDTO);
    }

    /**
     * DELETE  /offers/:id : delete the "id" offer.
     *
     * @param id the id of the offerDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/offers/{id}")
    @Timed
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        log.debug("REST request to delete Offer : {}", id);
        offerService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/offers?query=:query : search for the offer corresponding
     * to the query.
     *
     * @param query the query of the offer search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping("/_search/offers")
    @Timed
    public ResponseEntity<List<OfferDTO>> searchOffers(@RequestParam String query, Pageable pageable) {
        log.debug("REST request to search for a page of Offers for query {}", query);
        Page<OfferDTO> page = offerService.search(query, pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/offers");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
    
    /**
     * GET  /offers/:promoCode : get the "promoCode" offer.
     *
     * @param promoCode the promoCode of the offerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the offerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/offers/get-offer-by-promoCode/{promoCode}")
    @Timed
    public ResponseEntity<OfferDTO> getOfferByPromoCode(@PathVariable String promoCode) {
        log.debug("REST request to get Offer : {}", promoCode);
        Optional<OfferDTO> offerDTO = offerService.findOfferByPromoCode(promoCode);
        return ResponseUtil.wrapOrNotFound(offerDTO);
    }

    /**
     * POST  /offers/claimOffer/ : get the "promoCode" and claim the offer.
     *
     * @param orderModel the orderModel of the offerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the offerDTO, or with status 404 (Not Found)
     */
    @PostMapping("/offers/claimOffer")
    @Timed
    public String claimOffer(@RequestBody OrderModel orderModel) {
        log.debug("REST request to claim Offer : {}", orderModel.getPromoCode());
        offerService.claimOffer(orderModel);
        return "success";
        
    }
}
