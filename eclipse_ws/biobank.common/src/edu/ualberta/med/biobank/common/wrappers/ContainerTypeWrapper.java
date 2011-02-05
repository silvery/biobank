package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.exception.BiobankQueryResultSizeException;
import edu.ualberta.med.biobank.common.peer.AliquotPeer;
import edu.ualberta.med.biobank.common.peer.AliquotPositionPeer;
import edu.ualberta.med.biobank.common.peer.ContainerPeer;
import edu.ualberta.med.biobank.common.peer.ContainerPositionPeer;
import edu.ualberta.med.biobank.common.peer.ContainerTypePeer;
import edu.ualberta.med.biobank.common.peer.SampleTypePeer;
import edu.ualberta.med.biobank.common.security.User;
import edu.ualberta.med.biobank.common.util.RowColPos;
import edu.ualberta.med.biobank.common.wrappers.internal.CapacityWrapper;
import edu.ualberta.med.biobank.model.AliquotPosition;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;
import edu.ualberta.med.biobank.model.ContainerPosition;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.SampleType;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.ValueNotSetException;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class ContainerTypeWrapper extends ModelWrapper<ContainerType> {

    private Set<ContainerTypeWrapper> deletedChildTypes = new HashSet<ContainerTypeWrapper>();

    private Set<SampleTypeWrapper> deletedSampleTypes = new HashSet<SampleTypeWrapper>();

    public ContainerTypeWrapper(WritableApplicationService appService,
        ContainerType wrappedObject) {
        super(appService, wrappedObject);
    }

    public ContainerTypeWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    protected List<String> getPropertyChangeNames() {
        return ContainerTypePeer.PROP_NAMES;
    }

    @Override
    protected void persistChecks() throws BiobankException,
        ApplicationException {
        if (getSite() != null) {
            checkNoDuplicatesInSite(ContainerType.class,
                ContainerTypePeer.NAME.getName(), getName(), getSite().getId(),
                ContainerTypePeer.NAME.getName());
            checkNoDuplicatesInSite(ContainerType.class,
                ContainerTypePeer.NAME_SHORT.getName(), getNameShort(),
                getSite().getId(), ContainerTypePeer.NAME_SHORT.getName());
        }
        if (getCapacity() == null) {
            throw new ValueNotSetException("capacity");
        }
        if (getChildLabelingScheme() != null) {
            // should throw error if labeling scheme too small for container
            if (!ContainerLabelingSchemeWrapper.checkBounds(appService,
                getChildLabelingScheme(), getCapacity().getRowCapacity(),
                getCapacity().getColCapacity()))
                throw new BiobankCheckException("Labeling scheme cannot label "
                    + getCapacity().getRowCapacity() + " rows and "
                    + getCapacity().getColCapacity() + " columns.");
        }
        if (!isNew()) {
            boolean exists = isUsedByContainers();
            ContainerType oldObject = getObjectFromDatabase();
            checkNewCapacity(oldObject, exists);
            checkTopLevel(oldObject, exists);
            checkLabelingScheme(oldObject, exists);
            checkDeletedChildContainerTypes();
            checkDeletedSampleTypes();
        }
    }

    private static final String DELETED_SAMPLE_TYPES_QRY = "from "
        + AliquotPosition.class.getName()
        + " as ap inner join ap."
        + AliquotPositionPeer.CONTAINER.getName()
        + " as aparent where aparent."
        + Property.concatNames(ContainerPeer.CONTAINER_TYPE,
            ContainerTypePeer.ID)
        + "=? and ap."
        + Property.concatNames(AliquotPositionPeer.ALIQUOT,
            AliquotPeer.SAMPLE_TYPE, SampleTypePeer.ID) + " in (";

    private void checkDeletedSampleTypes() throws ApplicationException,
        BiobankCheckException {
        if (deletedSampleTypes.size() > 0) {
            List<Integer> ids = new ArrayList<Integer>();
            for (SampleTypeWrapper type : deletedSampleTypes) {
                ids.add(type.getId());
            }
            StringBuffer sb = new StringBuffer(DELETED_SAMPLE_TYPES_QRY)
                .append(StringUtils.join(ids, ',')).append(")");
            List<Object> results = appService.query(new HQLCriteria(sb
                .toString(), Arrays.asList(new Object[] { getId() })));
            if (results.size() != 0) {
                throw new BiobankCheckException(
                    "Unable to remove sample type. This parent/child relationship "
                        + "exists in database. Remove all instances before attempting to "
                        + "delete a sample type.");
            }
        }
    }

    private static final String DELETED_CONTAINER_TYPES_QRY = "from "
        + ContainerPosition.class.getName()
        + " as cp inner join cp."
        + ContainerPositionPeer.PARENT_CONTAINER.getName()
        + " as cparent where cparent."
        + Property.concatNames(ContainerPeer.CONTAINER_TYPE,
            ContainerTypePeer.ID)
        + "=? and cp."
        + Property.concatNames(ContainerPositionPeer.CONTAINER,
            ContainerPeer.CONTAINER_TYPE, ContainerTypePeer.ID) + " in (";

    private void checkDeletedChildContainerTypes()
        throws BiobankCheckException, ApplicationException {
        if (deletedChildTypes.size() > 0) {
            List<Integer> ids = new ArrayList<Integer>();
            for (ContainerTypeWrapper type : deletedChildTypes) {
                ids.add(type.getId());
            }
            StringBuffer sb = new StringBuffer(DELETED_CONTAINER_TYPES_QRY)
                .append(StringUtils.join(ids, ',')).append(")");
            List<Object> results = appService.query(new HQLCriteria(sb
                .toString(), Arrays.asList(new Object[] { getId() })));
            if (results.size() != 0) {
                throw new BiobankCheckException(
                    "Unable to remove child type. This parent/child relationship "
                        + "exists in database. Remove all instances before attempting to "
                        + "delete a child type.");
            }
        }
    }

    @Override
    public Class<ContainerType> getWrappedClass() {
        return ContainerType.class;
    }

    public Collection<ContainerTypeWrapper> getChildrenRecursively()
        throws ApplicationException {
        List<ContainerTypeWrapper> allChildren = new ArrayList<ContainerTypeWrapper>();
        List<ContainerTypeWrapper> children = getChildContainerTypeCollection();
        if (children != null) {
            for (ContainerTypeWrapper type : children) {
                allChildren.addAll(type.getChildrenRecursively());
                allChildren.add(type);
            }
        }
        return allChildren;
    }

    @Override
    public void deleteDependencies() throws Exception {
        // should remove this containerType from its parents
        for (ContainerTypeWrapper parent : getParentContainerTypes()) {
            parent.removeChildContainers(Arrays.asList(this));
            parent.persist();
        }
    }

    @Override
    protected void deleteChecks() throws BiobankException, ApplicationException {
        if (isUsedByContainers()) {
            throw new BiobankCheckException("Unable to delete container type "
                + getName() + ". A container of this type exists in storage."
                + " Remove all instances before deleting this type.");
        }
    }

    private static final String IS_USED_BY_CONTAINERS_QRY = "select count(c) from "
        + Container.class.getName()
        + " as c where c."
        + ContainerPeer.CONTAINER_TYPE.getName() + "=?";

    public boolean isUsedByContainers() throws ApplicationException,
        BiobankException {
        HQLCriteria c = new HQLCriteria(IS_USED_BY_CONTAINERS_QRY,
            Arrays.asList(new Object[] { wrappedObject }));
        List<Long> results = appService.query(c);
        if (results.size() != 1) {
            throw new BiobankQueryResultSizeException();
        }
        return results.get(0) > 0;
    }

    private static final String PARENT_CONTAINER_TYPES_QRY = "select ct from "
        + ContainerType.class.getName() + " as ct inner join ct."
        + ContainerTypePeer.CHILD_CONTAINER_TYPE_COLLECTION.getName()
        + " as child where child." + ContainerTypePeer.ID.getName() + "=?";

    public List<ContainerTypeWrapper> getParentContainerTypes()
        throws ApplicationException {
        HQLCriteria c = new HQLCriteria(PARENT_CONTAINER_TYPES_QRY,
            Arrays.asList(new Object[] { wrappedObject.getId() }));
        List<ContainerType> results = appService.query(c);
        return transformToWrapperList(appService, results);
    }

    public String getName() {
        return getProperty(ContainerTypePeer.NAME);
    }

    public void setName(String name) {
        setProperty(ContainerTypePeer.NAME, name);
    }

    public String getNameShort() {
        return getProperty(ContainerTypePeer.NAME_SHORT);
    }

    public void setNameShort(String nameShort) {
        setProperty(ContainerTypePeer.NAME_SHORT, nameShort);
    }

    public String getComment() {
        return getProperty(ContainerTypePeer.COMMENT);
    }

    public void setComment(String comment) {
        setProperty(ContainerTypePeer.COMMENT, comment);
    }

    public Boolean getTopLevel() {
        return getProperty(ContainerTypePeer.TOP_LEVEL);
    }

    public void setTopLevel(Boolean topLevel) {
        setProperty(ContainerTypePeer.TOP_LEVEL, topLevel);
    }

    public Double getDefaultTemperature() {
        return getProperty(ContainerTypePeer.DEFAULT_TEMPERATURE);
    }

    public void setDefaultTemperature(Double temperature) {
        setProperty(ContainerTypePeer.DEFAULT_TEMPERATURE, temperature);
    }

    public ActivityStatusWrapper getActivityStatus() {
        return getWrappedProperty(ContainerTypePeer.ACTIVITY_STATUS,
            ActivityStatusWrapper.class);
    }

    public void setActivityStatus(ActivityStatusWrapper activityStatus) {
        setWrappedProperty(ContainerTypePeer.ACTIVITY_STATUS, activityStatus);
    }

    private void setSampleTypeCollection(Collection<SampleType> allTypeObjects,
        List<SampleTypeWrapper> allTypeWrappers) {
        Collection<SampleType> oldTypes = wrappedObject
            .getSampleTypeCollection();
        wrappedObject.setSampleTypeCollection(allTypeObjects);
        propertyChangeSupport.firePropertyChange("sampleTypeCollection",
            oldTypes, allTypeObjects);
        propertiesMap.put("sampleTypeCollection", allTypeWrappers);
    }

    public void addSampleTypes(List<SampleTypeWrapper> newSampleTypes) {
        if (newSampleTypes != null && newSampleTypes.size() > 0) {
            Collection<SampleType> allTypeObjects = new HashSet<SampleType>();
            List<SampleTypeWrapper> allTypeWrappers = new ArrayList<SampleTypeWrapper>();
            // already added types
            List<SampleTypeWrapper> currentList = getSampleTypeCollection();
            if (currentList != null) {
                for (SampleTypeWrapper type : currentList) {
                    allTypeObjects.add(type.getWrappedObject());
                    allTypeWrappers.add(type);
                }
            }
            // new types
            for (SampleTypeWrapper type : newSampleTypes) {
                allTypeObjects.add(type.getWrappedObject());
                allTypeWrappers.add(type);
                deletedSampleTypes.remove(type);
            }
            setSampleTypeCollection(allTypeObjects, allTypeWrappers);
        }
    }

    public void removeSampleTypes(List<SampleTypeWrapper> typesToRemove) {
        if (typesToRemove != null && typesToRemove.size() > 0) {
            deletedSampleTypes.addAll(typesToRemove);
            Collection<SampleType> allTypeObjects = new HashSet<SampleType>();
            List<SampleTypeWrapper> allTypeWrappers = new ArrayList<SampleTypeWrapper>();
            // already added types
            List<SampleTypeWrapper> currentList = getSampleTypeCollection();
            if (currentList != null) {
                for (SampleTypeWrapper type : currentList) {
                    if (!deletedSampleTypes.contains(type)) {
                        allTypeObjects.add(type.getWrappedObject());
                        allTypeWrappers.add(type);
                    }
                }
            }
            setSampleTypeCollection(allTypeObjects, allTypeWrappers);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SampleTypeWrapper> getSampleTypeCollection() {
        List<SampleTypeWrapper> sampleTypeCollection = (List<SampleTypeWrapper>) propertiesMap
            .get("sampleTypeCollection");
        if (sampleTypeCollection == null) {
            Collection<SampleType> children = wrappedObject
                .getSampleTypeCollection();
            if (children != null) {
                sampleTypeCollection = new ArrayList<SampleTypeWrapper>();
                for (SampleType type : children) {
                    sampleTypeCollection.add(new SampleTypeWrapper(appService,
                        type));
                }
                propertiesMap.put("sampleTypeCollection", sampleTypeCollection);
            }
        }
        return sampleTypeCollection;
    }

    public Set<SampleTypeWrapper> getSampleTypesRecursively()
        throws ApplicationException {
        Set<SampleTypeWrapper> sampleTypes = new HashSet<SampleTypeWrapper>();
        List<SampleTypeWrapper> sampleSubSet = getSampleTypeCollection();
        if (sampleSubSet != null)
            sampleTypes.addAll(sampleSubSet);
        for (ContainerTypeWrapper type : getChildContainerTypeCollection()) {
            sampleTypes.addAll(type.getSampleTypesRecursively());
        }
        return sampleTypes;
    }

    private void setChildContainerTypes(
        Collection<ContainerType> allTypeObjects,
        List<ContainerTypeWrapper> allTypeWrappers) {
        Collection<ContainerType> oldContainerTypes = wrappedObject
            .getChildContainerTypeCollection();
        wrappedObject.setChildContainerTypeCollection(allTypeObjects);
        propertyChangeSupport.firePropertyChange(
            "childContainerTypeCollection", oldContainerTypes, allTypeObjects);
        propertiesMap.put("childContainerTypeCollection", allTypeWrappers);
    }

    public void addChildContainerTypes(
        List<ContainerTypeWrapper> newContainerTypes) {
        if (newContainerTypes != null && newContainerTypes.size() > 0) {
            Collection<ContainerType> allTypeObjects = new HashSet<ContainerType>();
            List<ContainerTypeWrapper> allTypesWrappers = new ArrayList<ContainerTypeWrapper>();
            // already added types
            List<ContainerTypeWrapper> currentList = getChildContainerTypeCollection();
            if (currentList != null) {
                for (ContainerTypeWrapper type : currentList) {
                    allTypeObjects.add(type.getWrappedObject());
                    allTypesWrappers.add(type);
                }
            }
            // new types
            for (ContainerTypeWrapper type : newContainerTypes) {
                allTypeObjects.add(type.getWrappedObject());
                allTypesWrappers.add(type);
                deletedChildTypes.remove(type);
            }
            setChildContainerTypes(allTypeObjects, allTypesWrappers);
        }
    }

    public void removeChildContainers(List<ContainerTypeWrapper> typesToRemove) {
        if (typesToRemove != null && typesToRemove.size() > 0) {
            deletedChildTypes.addAll(typesToRemove);
            Collection<ContainerType> allTypeObjects = new HashSet<ContainerType>();
            List<ContainerTypeWrapper> allTypesWrappers = new ArrayList<ContainerTypeWrapper>();
            // already added types
            List<ContainerTypeWrapper> currentList = getChildContainerTypeCollection();
            if (currentList != null) {
                for (ContainerTypeWrapper type : currentList) {
                    if (!deletedChildTypes.contains(type)) {
                        allTypeObjects.add(type.getWrappedObject());
                        allTypesWrappers.add(type);
                    }
                }
            }
            setChildContainerTypes(allTypeObjects, allTypesWrappers);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ContainerTypeWrapper> getChildContainerTypeCollection() {
        List<ContainerTypeWrapper> childContainerTypeCollection = (List<ContainerTypeWrapper>) propertiesMap
            .get("childContainerTypeCollection");
        if (childContainerTypeCollection == null) {
            Collection<ContainerType> children = wrappedObject
                .getChildContainerTypeCollection();
            if (children != null) {
                childContainerTypeCollection = transformToWrapperList(
                    appService, children);
                propertiesMap.put("childContainerTypeCollection",
                    childContainerTypeCollection);
            }
        }
        return childContainerTypeCollection;
    }

    public void setSite(SiteWrapper site) {
        propertiesMap.put("site", site);
        Site oldSite = wrappedObject.getSite();
        Site newSite = site.getWrappedObject();
        wrappedObject.setSite(newSite);
        propertyChangeSupport.firePropertyChange("site", oldSite, newSite);
    }

    public SiteWrapper getSite() {
        SiteWrapper site = (SiteWrapper) propertiesMap.get("site");
        if (site == null) {
            Site s = wrappedObject.getSite();
            if (s == null)
                return null;
            site = new SiteWrapper(appService, s);
            propertiesMap.put("site", site);
        }
        return site;
    }

    private CapacityWrapper getCapacity() {
        Capacity capacity = wrappedObject.getCapacity();
        if (capacity == null) {
            return null;
        }
        return new CapacityWrapper(appService, capacity);
    }

    private void setCapacity(CapacityWrapper capacity) {
        Capacity oldCapacity = wrappedObject.getCapacity();
        Capacity newCapacity = capacity.wrappedObject;
        wrappedObject.setCapacity(newCapacity);
        propertyChangeSupport.firePropertyChange("capacity", oldCapacity,
            newCapacity);
    }

    public Integer getRowCapacity() {
        CapacityWrapper capacity = getCapacity();
        if (capacity == null) {
            return null;
        }
        return capacity.getRowCapacity();
    }

    public Integer getColCapacity() {
        CapacityWrapper capacity = getCapacity();
        if (capacity == null) {
            return null;
        }
        return capacity.getColCapacity();
    }

    public void setRowCapacity(Integer maxRows) {
        Integer old = getRowCapacity();
        CapacityWrapper capacity = getCapacity();
        if (capacity == null) {
            capacity = new CapacityWrapper(appService, new Capacity());
        }
        capacity.setRow(maxRows);
        setCapacity(capacity);
        propertyChangeSupport.firePropertyChange("rowCapacity", old, maxRows);
    }

    public void setColCapacity(Integer maxCols) {
        Integer old = getColCapacity();
        CapacityWrapper capacity = getCapacity();
        if (capacity == null) {
            capacity = new CapacityWrapper(appService, new Capacity());
        }
        capacity.setCol(maxCols);
        setCapacity(capacity);
        propertyChangeSupport.firePropertyChange("colCapacity", old, maxCols);
    }

    public void setChildLabelingScheme(Integer id) throws ApplicationException {
        if (id == null)
            return;
        setChildLabelingScheme(ContainerLabelingSchemeWrapper
            .getLabelingSchemeById(appService, id));
    }

    public void setChildLabelingSchemeName(String name) throws Exception {
        if (name == null)
            return;
        for (ContainerLabelingSchemeWrapper scheme : ContainerLabelingSchemeWrapper
            .getAllLabelingSchemesMap(appService).values()) {
            if (scheme.getName().equals(name)) {
                setChildLabelingScheme(scheme);
                return;
            }
        }
        throw new Exception("labeling scheme with name \"" + name
            + "\" does not exist");
    }

    private void setChildLabelingScheme(ContainerLabelingSchemeWrapper scheme) {
        ContainerLabelingScheme oldLbl = wrappedObject.getChildLabelingScheme();
        ContainerLabelingScheme newLbl = null;
        if (scheme != null) {
            newLbl = scheme.getWrappedObject();
        }
        wrappedObject.setChildLabelingScheme(newLbl);
        propertyChangeSupport.firePropertyChange("childLabelingScheme", oldLbl,
            newLbl);
    }

    public Integer getChildLabelingScheme() {
        ContainerLabelingScheme scheme = wrappedObject.getChildLabelingScheme();
        if (scheme == null) {
            return null;
        }
        return scheme.getId();
    }

    public String getChildLabelingSchemeName() {
        ContainerLabelingScheme scheme = wrappedObject.getChildLabelingScheme();
        if (scheme == null) {
            return null;
        }
        return scheme.getName();
    }

    /**
     * Check if we can use the new capacity
     */
    private void checkNewCapacity(ContainerType oldObject,
        boolean existsContainersWithType) throws BiobankCheckException {
        CapacityWrapper currentCapacity = getCapacity();
        Capacity dbCapacity = oldObject.getCapacity();
        if (!(currentCapacity.getRowCapacity().equals(
            dbCapacity.getRowCapacity()) && currentCapacity.getColCapacity()
            .equals(dbCapacity.getColCapacity())) && existsContainersWithType) {
            throw new BiobankCheckException(
                "Unable to alter dimensions. A container of this type exists "
                    + "in storage. Remove all instances before attempting to "
                    + "modify this container type.");
        }
    }

    private void checkTopLevel(ContainerType oldObject,
        boolean existsContainersWithType) throws BiobankCheckException {
        if (((getTopLevel() == null && oldObject.getTopLevel() != null)
            || (getTopLevel() != null && oldObject.getTopLevel() == null) || (getTopLevel() != null
            && oldObject.getTopLevel() != null && !getTopLevel().equals(
            oldObject.getTopLevel())))
            && existsContainersWithType) {
            throw new BiobankCheckException(
                "Unable to change the \"Top Level\" property. A container "
                    + "requiring this property exists in storage. Remove all "
                    + "instances before attempting to modify this container type.");
        }
    }

    private void checkLabelingScheme(ContainerType oldObject,
        boolean existsContainersWithType) throws BiobankCheckException {
        ContainerTypeWrapper oldWrapper = new ContainerTypeWrapper(appService,
            oldObject);
        if (getChildLabelingScheme() == null
            && oldWrapper.getChildLabelingScheme() == null) {
            return;
        }
        if (getChildLabelingScheme() == null
            || oldWrapper.getChildLabelingScheme() == null
            || !getChildLabelingScheme().equals(
                oldWrapper.getChildLabelingScheme())
            && existsContainersWithType) {
            throw new BiobankCheckException(
                "Unable to change the \"Child Labeling scheme\" property. "
                    + "A container requiring this property exists in storage. "
                    + "Remove all instances before attempting to modify this "
                    + "container type.");
        }
    }

    public static List<ContainerTypeWrapper> getTopContainerTypesInSite(
        WritableApplicationService appService, SiteWrapper site)
        throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria("from "
            + ContainerType.class.getName()
            + " where site.id = ? and topLevel=true",
            Arrays.asList(new Object[] { site.getId() }));
        List<ContainerType> types = appService.query(criteria);
        return transformToWrapperList(appService, types);
    }

    public static List<ContainerTypeWrapper> transformToWrapperList(
        WritableApplicationService appService,
        Collection<ContainerType> containerTypes) {
        List<ContainerTypeWrapper> list = new ArrayList<ContainerTypeWrapper>();
        for (ContainerType type : containerTypes) {
            list.add(new ContainerTypeWrapper(appService, type));
        }
        return new ArrayList<ContainerTypeWrapper>(list);
    }

    /**
     * Get containers types defined in a site. if useStrictName is true, then
     * the container type name should be exactly containerName, otherwise, it
     * should contain containerName as a substring in the name.
     */
    public static List<ContainerTypeWrapper> getContainerTypesInSite(
        WritableApplicationService appService, SiteWrapper siteWrapper,
        String containerName, boolean useStrictName)
        throws ApplicationException {
        String nameComparison = "name =";
        String containerNameParameter = containerName;
        if (!useStrictName) {
            nameComparison = "lower(name) like";
            containerNameParameter = "%" + containerName.toLowerCase() + "%";
        }
        String query = "from " + ContainerType.class.getName()
            + " where site = ? and " + nameComparison + " ?";
        HQLCriteria criteria = new HQLCriteria(query,
            Arrays.asList(new Object[] { siteWrapper.getWrappedObject(),
                containerNameParameter }));
        List<ContainerType> containerTypes = appService.query(criteria);
        return transformToWrapperList(appService, containerTypes);
    }

    /**
     * Get containers types with the given capacity in the given site. The
     * container types returned are ones that can only hold aliquots.
     */
    public static List<ContainerTypeWrapper> getContainerTypesByCapacity(
        WritableApplicationService appService, SiteWrapper siteWrapper,
        int maxRows, int maxCols) throws ApplicationException {
        String query = "select ct from "
            + ContainerType.class.getName()
            + " as ct join ct.capacity as cap"
            + " where ct.site = ? and cap.rowCapacity = ?"
            + " and cap.colCapacity = ? and ct.sampleTypeCollection is not empty"
            + " and ct.childContainerTypeCollection is empty";
        HQLCriteria criteria = new HQLCriteria(query,
            Arrays.asList(new Object[] { siteWrapper.getWrappedObject(),
                maxRows, maxCols }));
        List<ContainerType> containerTypes = appService.query(criteria);
        return transformToWrapperList(appService, containerTypes);
    }

    public static List<ContainerTypeWrapper> getContainerTypesPallet96(
        WritableApplicationService appService, SiteWrapper siteWrapper)
        throws ApplicationException {
        return getContainerTypesByCapacity(appService, siteWrapper,
            RowColPos.PALLET_96_ROW_MAX, RowColPos.PALLET_96_COL_MAX);
    }

    /**
     * get count of container which type is this
     */
    public long getContainersCount() throws ApplicationException,
        BiobankException {
        HQLCriteria c = new HQLCriteria("select count(*) from "
            + Container.class.getName() + " where containerType.id=?",
            Arrays.asList(new Object[] { getId() }));
        List<Long> results = appService.query(c);
        if (results.size() != 1) {
            throw new BiobankQueryResultSizeException();
        }
        return results.get(0);
    }

    @Override
    public int compareTo(ModelWrapper<ContainerType> type) {
        if (type instanceof ContainerTypeWrapper) {
            String c1Name = wrappedObject.getName();
            String c2Name = type.wrappedObject.getName();
            return ((c1Name.compareTo(c2Name) > 0) ? 1
                : (c1Name.equals(c2Name) ? 0 : -1));
        }
        return 0;
    }

    @Override
    public String toString() {
        return getName() + " (" + getNameShort() + ")";
    }

    @Override
    protected void resetInternalFields() {
        deletedChildTypes.clear();
        deletedSampleTypes.clear();
    }

    public String getPositionString(RowColPos position) {
        return ContainerLabelingSchemeWrapper.getPositionString(position,
            getChildLabelingScheme(), getRowCapacity(), getColCapacity());
    }

    public RowColPos getRowColFromPositionString(String position)
        throws Exception {
        return ContainerLabelingSchemeWrapper.getRowColFromPositionString(
            getAppService(), position, getChildLabelingScheme(),
            getRowCapacity(), getColCapacity());
    }

    @Override
    public SiteWrapper getSiteLinkedToObject() {
        return getSite();
    }

    @Override
    public boolean checkSpecificAccess(User user, Integer siteId) {
        return user.isSiteAdministrator(siteId);
    }
}
