/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.model;

import org.apache.log4j.Logger;
import org.candlepin.auth.interceptor.EnforceAccessControl;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.Statistics;

import com.google.inject.persist.Transactional;

/**
 * interact with Products.
 */
public class ProductCurator extends AbstractHibernateCurator<Product> {
    private static Logger log = Logger
        .getLogger(ProductCurator.class);

    /**
     * default ctor
     */
    public ProductCurator() {
        super(Product.class);
    }

    /**
     * @param name the product name to lookup
     * @return the Product which matches the given name.
     */
    @Transactional
    public Product lookupByName(String name) {
        Product p = (Product) currentSession().createCriteria(Product.class)
            .add(Restrictions.eq("name", name)).uniqueResult();
        Statistics stats = currentSession().getSessionFactory().getStatistics();
        log.info("##### CACHE STATS (NAME): " + stats);
        return p;
    }

    /**
     * @param id product id to lookup
     * @return the Product which matches the given id.
     */
    @Transactional
    public Product lookupById(String id) {

        //Product p = (Product) currentSession()
        //    .createCriteria(Product.class)
        //    .add(Restrictions.naturalId().set("id", id))
        //    .setCacheable(false).uniqueResult();
        Product p = (Product) currentSession()
            .createQuery("from Product p where p.id = :product_id")
            .setCacheable(true)
            .setParameter("product_id", id).uniqueResult();

        Statistics stats = currentSession().getSessionFactory().getStatistics();
        log.info("##### CACHE STATS (ID): " + stats);
        return p;
    }

    /**
     * Create the given product if it does not already exist, otherwise update
     * existing product.
     *
     * @param p Product to create or update.
     */
    public void createOrUpdate(Product p) {
        Product existing = lookupById(p.getId());
        if (existing == null) {
            create(p);
            return;
        }

        for (ProductAttribute attr : p.getAttributes()) {
            attr.setProduct(p);
        }

        merge(p);
    }

    @Transactional
    @EnforceAccessControl
    public Product create(Product entity) {

        /*
         * Ensure all referenced ProductAttributes are correctly pointing to
         * this product. This is useful for products being created from incoming
         * json.
         */
        for (ProductAttribute attr : entity.getAttributes()) {
            attr.setProduct(entity);
        }

        return super.create(entity);
    }

    @Transactional
    @EnforceAccessControl
    public void removeProductContent(Product prod, Content content) {
        for (ProductContent pc : prod.getProductContent()) {
            if (content.getId().equals(pc.getContent().getId())) {
                prod.getProductContent().remove(pc);
                break;
            }
        }
        merge(prod);
    }

    public boolean productHasSubscriptions(Product prod) {
        String poolString = "select id from Subscription s" +
            " where s.product.id = :prodId";
        Query poolQuery = currentSession().createQuery(poolString).setString(
            "prodId", prod.getId());
        return poolQuery.list().size() > 0;
    }
}
