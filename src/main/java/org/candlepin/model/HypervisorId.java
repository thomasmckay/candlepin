/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.UniqueConstraint;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

/**
 * HypervisorId represents a hypervisor host, unique per organization
 *
 * This ID is generated by the hypervisor.  In most cases it is a uuid,
 * so we can be fairly sure it is unique, however in some cases such
 * as openstack it may be a hostname.
 *
 * This structure allows us to tag a consumer (hypervisor type or otherwise)
 * with a hypervisorId so that hypervisorCheckIn can update its guest list
 * without additional info or lookups.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "cp_consumer_hypervisor",
    uniqueConstraints = @UniqueConstraint(
        name = "cp_consumer_hypervisor_ukey",
        columnNames = {"owner_id", "hypervisor_id"}))
public class HypervisorId extends AbstractHibernateObject {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 32)
    private String id;

    @Column(name = "hypervisor_id", nullable = false)
    @Index(name = "idx_hypervisor_id")
    private String hypervisorId;

    @OneToOne
    @ForeignKey(name = "fk_hypervisor_consumer")
    @JoinColumn(nullable = false, unique = true)
    @XmlTransient
    private Consumer consumer;

    @ManyToOne
    @ForeignKey(name = "fk_hypervisor_owner")
    @JoinColumn(nullable = false)
    @Index(name = "idx_hypervisor_owner_fk")
    @XmlTransient
    private Owner owner;

    public HypervisorId() {
    }

    public HypervisorId(String hypervisorId) {
        this.setHypervisorId(hypervisorId);
    }

    public HypervisorId(Consumer consumer, String hypervisorId) {
        this(hypervisorId);
        this.setConsumer(consumer); // Also sets owner
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the hypervisorId
     */
    public String getHypervisorId() {
        return hypervisorId;
    }

    /**
     * @param hypervisorId the hypervisorId to set
     */
    public void setHypervisorId(String hypervisorId) {
        // Hypervisor uuid is case insensitive, we need to force it to lower
        // case in order to enforce the unique hypervisorId per org constraint
        if (hypervisorId != null) {
            hypervisorId = hypervisorId.toLowerCase();
        }
        this.hypervisorId = hypervisorId;
    }

    /**
     * @return the consumer
     */
    @XmlTransient
    public Consumer getConsumer() {
        return consumer;
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
        this.owner = consumer.getOwner();
    }

    /**
     * @return the owner
     */
    @XmlTransient
    public Owner getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }
}
