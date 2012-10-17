package edu.ualberta.med.biobank.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

import edu.ualberta.med.biobank.validator.constraint.Unique;
import edu.ualberta.med.biobank.validator.group.PrePersist;

/**
 * Determines which {@link SpecimenLinkAnnotationType}s either can or must
 * (determined by {@link #isRequired()}) be recorded on which
 * {@link SpecimenLinkType}s.
 * 
 * @author Jonathan Ferland
 */
@Audited
@Entity
@Table(name = "SPECIMEN_LINK_TYPE_ANNOTATION_TYPE")
@Unique(properties = { "linkType", "annotationType" }, groups = PrePersist.class)
public class SpecimenLinkTypeAnnotationType
    extends VersionedLongIdModel {
    private static final long serialVersionUID = 1L;

    private SpecimenLinkType linkType;
    private SpecimenLinkAnnotationType annotationType;
    private Boolean required;

    @NaturalId
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "{SpecimenLinkTypeAnnotationType.linkType.NotNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPECIMEN_LINK_TYPE_ID", nullable = false)
    public SpecimenLinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(SpecimenLinkType linkType) {
        this.linkType = linkType;
    }

    @NaturalId
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "{SpecimenLinkTypeAnnotationType.annotationType.NotNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPECIMEN_LINK_ANNOTATION_TYPE", nullable = false)
    public SpecimenLinkAnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(SpecimenLinkAnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * @return true if a value for {@link #getAnnotationType()} <em>must</em> be
     *         recorded whenever a {@link SpecimenLink} of type
     *         {@link #getLinkType()} is created, otherwise false when a value
     *         is optional.
     */
    @NotNull(message = "{AnnotationType.required.NotNull}")
    @Column(name = "IS_REQUIRED", nullable = false)
    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
