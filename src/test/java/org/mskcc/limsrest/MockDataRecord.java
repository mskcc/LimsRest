package org.mskcc.limsrest;

import com.velox.api.access.DataRecordACL;
import com.velox.api.datafield.DataFields;
import com.velox.api.datarecord.*;
import com.velox.api.datarecord.paging.DataRecordMultiTypePageId;
import com.velox.api.datarecord.paging.DataRecordPageCriteria;
import com.velox.api.datarecord.paging.DataRecordPageId;
import com.velox.api.datarecord.paging.DataRecordPageResult;
import com.velox.api.exception.recoverability.serverexception.UnrecoverableServerException;
import com.velox.api.io.RemoteInputStream;
import com.velox.api.io.RemoteOutputStream;
import com.velox.api.user.User;
import com.velox.api.util.ClientCallbackRMI;
import com.velox.api.util.ServerException;
import com.velox.util.time.DateRange;

import java.rmi.RemoteException;
import java.util.*;

public class MockDataRecord implements DataRecordProxy {
    // only getDataField & setDataField currently implemented
    HashMap<String, Object> map = new HashMap<>();

    public Object getDataField(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return map.get(dataFieldName);
    }
    @Override
    public void setDataField(DataRecord dataRecord, String dataFieldName, Object newValue, User user) throws IoError, InvalidValue, NotFound, RemoteException, ServerException {
        map.put(dataFieldName, newValue);
    }

    @Override
    public String getName(DataRecord dataRecord, User user) throws RemoteException {
        return null;
    }

    @Override
    public boolean hasChildren(DataRecord dataRecord, User user) throws IoError, RemoteException, ServerException {
        return false;
    }

    @Override
    public List<DataRecord> getChildren(DataRecord dataRecord, User user) throws IoError, RemoteException, UnrecoverableServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordMultiTypePageId, DataRecord> getChildren(DataRecord dataRecord, DataRecordPageCriteria<DataRecordMultiTypePageId> pageCriteria, User user) throws IoError, RemoteException, UnrecoverableServerException {
        return null;
    }

    @Override
    public DataRecordAttributes getAttributes(DataRecord dataRecord, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public DataRecordAttributes[] getChildAttributesList(DataRecord dataRecord, User user) throws IoError, RemoteException, ServerException {
        return new DataRecordAttributes[0];
    }

    @Override
    public DataRecord addChild(DataRecord dataRecord, DataFields dataFields, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public DataRecord addChild(DataRecord dataRecord, String dataTypeName, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public void removeChild(DataRecord dataRecord, DataRecord child, ClientCallbackRMI clientCallbackRMI, User user) throws IoError, NotFound, RemoteException, ServerException {

    }

    @Override
    public DataRecord copyDataRecord(DataRecord fromRecord, DataRecord toRecord, boolean withChildren, User user) throws IoError, AlreadyExists, InvalidValue, RemoteException {
        return null;
    }

    @Override
    public DataFields getDataFields(DataRecord dataRecord, User user) throws IoError, RemoteException {
        return null;
    }



    @Override
    public boolean verifyDataFields(DataRecord dataRecord, List<String> dataFieldNameList, boolean recurseChildren, ClientCallbackRMI clientCallbackRMI, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public boolean hasAccess(DataRecord dataRecord, User user, int accessTypeId) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public void checkAccess(DataRecord dataRecord, User user, int accessTypeId) throws ServerException, RemoteException {

    }

    @Override
    public boolean isReadOnly(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public List<DataRecord> getChildrenOfType(DataRecord dataRecord, String dataTypeName, User user) throws IoError, RemoteException, UnrecoverableServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordPageId, DataRecord> getChildrenOfType(DataRecord dataRecord, String childDataTypeName, DataRecordPageCriteria<DataRecordPageId> pageCriteria, User user) throws RemoteException, ServerException {
        return null;
    }

    @Override
    public DataRecord findChild(DataRecord dataRecord, String dataTypeName, String dataFieldName, Object dataFieldValue, User user) throws RemoteException {
        return null;
    }

    @Override
    public DataRecord findChild(DataRecord dataRecord, String dataTypeName, String dataFieldName1, Object dataFieldValue1, String dataFieldName2, Object dataFieldValue2, User user) throws RemoteException {
        return null;
    }

    @Override
    public boolean getBooleanVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return false;
    }

    @Override
    public int getIntegerVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public long getLongVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public double getDoubleVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getEnumVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getShortVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public String getStringVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getFileVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getSelectionVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public long getDateVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public DateRange getDateRangeVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public Object getValue(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public boolean getLastSavedBooleanVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return false;
    }

    @Override
    public String getLastSavedSelectionVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public int getLastSavedIntegerVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public long getLastSavedLongVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public double getLastSavedDoubleVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public short getLastSavedEnumVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public short getLastSavedShortVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public String getLastSavedStringVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public String getLastSavedFileVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public long getLastSavedDateVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return 0;
    }

    @Override
    public DateRange getLastSavedDateRangeVal(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public Object getLastSavedValue(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public Object getLastSavedDataField(DataRecord dataRecord, String dataFieldName) throws RemoteException {
        return null;
    }

    @Override
    public void setDataFields(DataRecord dataRecord, DataFields newDataFields, User user) throws IoError, InvalidValue, NotFound, RemoteException, ServerException {

    }

    @Override
    public List<DataRecord> getParentList(DataRecord dataRecord, User user) throws IoError, RemoteException, UnrecoverableServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordMultiTypePageId, DataRecord> getParentList(DataRecord dataRecord, DataRecordPageCriteria<DataRecordMultiTypePageId> pageCriteria, User user) throws IoError, RemoteException, UnrecoverableServerException {
        return null;
    }

    @Override
    public void delete(DataRecord dataRecord, ClientCallbackRMI clientCallbackRMI, User user, boolean recurse) throws IoError, NotFound, RemoteException, ServerException {

    }

    @Override
    public DataRecord addChild(DataRecord dataRecord, DataRecord childDataRecord, User user) throws AlreadyExists, NotFound, IoError, RemoteException, ServerException {
        return null;
    }

    @Override
    public List<DataRecord> getParentsOfType(DataRecord dataRecord, String parentDataTypeName, User user) throws IoError, RemoteException, ServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordPageId, DataRecord> getParentsOfType(DataRecord dataRecord, String parentDataTypeName, DataRecordPageCriteria<DataRecordPageId> pageCriteria, User user) throws IoError, RemoteException, ServerException {
        return null;
    }

    @Override
    public boolean isDeleted(DataRecord dataRecord) throws RemoteException {
        return false;
    }

    @Override
    public boolean[] getChildHasChildrenList(DataRecord dataRecord, User user) throws IoError, RemoteException, ServerException {
        return new boolean[0];
    }

    @Override
    public boolean isNew(DataRecord dataRecord) throws RemoteException {
        return false;
    }

    @Override
    public boolean isChanged(DataRecord dataRecord, String[] dataFieldNameList) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public boolean isChanged(DataRecord dataRecord, String dataFieldName) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public List<DataRecord> getAddedParentList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> getRemovedParentList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> getAddedChildList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> getRemovedChildList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public boolean hasPendingRelationChanges(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public List<DataRecord> getDescendantsOfType(DataRecord dataRecord, String descendantTypeName, User user) throws RemoteException, IoError, UnrecoverableServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordPageId, DataRecord> getDescendantsOfType(DataRecord dataRecord, String descendantTypeName, DataRecordPageCriteria<DataRecordPageId> pageCriteria, User user) throws RemoteException {
        return null;
    }

    @Override
    public DataRecordACL getDataRecordACL(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void setDataRecordACL(DataRecord dataRecord, DataRecordACL dataRecordACL, User authorizingUser) throws ServerException, RemoteException {

    }

    @Override
    public void setDataRecordACL(DataRecord dataRecord, DataRecordACL dataRecordACL, boolean forceInherit, User authorizingUser) throws ServerException, RemoteException {

    }

    @Override
    public void revertDataRecordACL(DataRecord dataRecord, User user) throws ServerException, RemoteException {

    }

    @Override
    public Map<String, Object> getFields(DataRecord dataRecord, User user) throws RemoteException {
        return null;
    }

    @Override
    public void setFields(DataRecord dataRecord, Map<String, Object> fieldMap, User user) throws ServerException, RemoteException {

    }

    @Override
    public DataRecord addChild(DataRecord dataRecord, String dataTypeName, Map<String, Object> fieldMap, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> addChildren(DataRecord dataRecord, String dataTypeName, int numberToAdd, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> addChildren(DataRecord dataRecord, String dataTypeName, List<Map<String, Object>> fieldMapList, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public void addChildrenDataPump(DataRecord dataRecord, String dataTypeName, List<Map<String, Object>> fieldMapList, User user) throws ServerException, RemoteException {

    }

    @Override
    public List<DataRecord> getAncestorsOfType(DataRecord dataRecord, String ancestorTypeName, User user) throws RemoteException, IoError, ServerException {
        return null;
    }

    @Override
    public DataRecordPageResult<DataRecordPageId, DataRecord> getAncestorsOfType(DataRecord dataRecord, String ancestorTypeName, DataRecordPageCriteria<DataRecordPageId> pageCriteria, User user) throws RemoteException {
        return null;
    }

    @Override
    public void addChildren(DataRecord dataRecord, List<DataRecord> childList, User user) throws AlreadyExists, NotFound, IoError, RemoteException, ServerException {

    }

    @Override
    public String getPickListVal(DataRecord dataRecord, String dataFieldName, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getLastSavedPickListVal(DataRecord dataRecord, String dataFieldName) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public DataRecord addChildIfNotExists(DataRecord dataRecord, DataRecord childRecord, User user) throws IoError, RemoteException, ServerException {
        return null;
    }

    @Override
    public boolean hasChild(DataRecord dataRecord, long childRecordId, User user) throws IoError, RemoteException, ServerException {
        return false;
    }

    @Override
    public boolean isAttachment(DataRecord dataRecord) throws RemoteException {
        return false;
    }

    @Override
    public boolean isAttachmentDataPresent(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public Long getAttachmentDataLength(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public Long getAttachmentDataLength(DataRecord dataRecord, User user, int version) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void setAttachmentData(DataRecord dataRecord, byte[] attachmentData, User user) throws ServerException, RemoteException {

    }

    @Override
    public void setDraftAttachmentData(DataRecord dataRecord, byte[] attachmentData, User user) throws ServerException, RemoteException {

    }

    @Override
    public byte[] getAttachmentData(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getAttachmentData(DataRecord dataRecord, User user, int version) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public RemoteInputStream openInputStreamForVersion(DataRecord dataRecord, long startPosition, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public RemoteInputStream openInputStreamForVersion(DataRecord dataRecord, long startPosition, int version, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public RemoteOutputStream openOutputStream(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public boolean isAttachmentChunkPresent(DataRecord dataRecord, String chunkHash, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public Set<String> getNonExistentAttachmentChunks(DataRecord dataRecord, Collection<String> chunkHashes, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<String> getAttachmentChunkHashes(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<String> getAttachmentDownloadUrls(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void setAttachmentChunkHashes(DataRecord dataRecord, List<String> chunkHashes, User user) throws ServerException, RemoteException {

    }

    @Override
    public RemoteInputStream openAttachmentChunkInputStream(DataRecord dataRecord, String chunkHash, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void uploadAttachmentChunk(DataRecord dataRecord, byte[] chunkData, User user) throws ServerException, RemoteException {

    }

    @Override
    public byte[] getAttachmentThumbnail(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getAttachmentThumbnail(DataRecord dataRecord, User user, int version) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public AttachmentAnnotation getAttachmentAnnotation(DataRecord dataRecord, int version, User user) throws RemoteException, ServerException {
        return null;
    }

    @Override
    public AttachmentAnnotation getAttachmentAnnotation(DataRecord dataRecord, User user) throws RemoteException, ServerException {
        return null;
    }

    @Override
    public void setAttachmentAnnotation(DataRecord dataRecord, AttachmentAnnotation annotation, User user) throws RemoteException, ServerException {

    }

    @Override
    public boolean isAttachmentAnnotationChanged(DataRecord dataRecord) throws RemoteException {
        return false;
    }

    @Override
    public boolean isAttachmentDataChanged(DataRecord dataRecord) throws RemoteException {
        return false;
    }

    @Override
    public List<AttachmentVersion> getAttachmentVersionList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void deleteAttachmentVersion(DataRecord dataRecord, int version, User user) throws ServerException, RemoteException {

    }

    @Override
    public void checkOut(DataRecord dataRecord, User user) throws ServerException, RemoteException {

    }

    @Override
    public void cancelCheckOut(DataRecord dataRecord, User user) throws ServerException, RemoteException {

    }

    @Override
    public void checkIn(DataRecord dataRecord, User user) throws ServerException, RemoteException {

    }

    @Override
    public void promoteDraft(DataRecord dataRecord, User user) throws ServerException, RemoteException {

    }

    @Override
    public void requestApproval(DataRecord dataRecord, String approvalReason, User user) throws ServerException, RemoteException {

    }

    @Override
    public void approve(DataRecord dataRecord, String approverComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public void reject(DataRecord dataRecord, String approverComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public DataRecordApproval getApproval(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecordApproval> getApprovalList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public DataRecordComment createComment(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void addComment(DataRecord dataRecord, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public void deleteComment(DataRecord dataRecord, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public void editComment(DataRecord dataRecord, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public List<DataRecordComment> getCommentList(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public byte[] getRecordImage(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public String getRecordImageId(DataRecord dataRecord, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void setRecordImage(DataRecord dataRecord, byte[] recordImageBytes, User user) throws ServerException, RemoteException {

    }

    @Override
    public boolean isRecordImageChanged(DataRecord dataRecord) throws RemoteException {
        return false;
    }
}