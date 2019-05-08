package org.mskcc.limsrest;

import com.velox.api.access.DataRecordACL;
import com.velox.api.datafield.DataFields;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ClientCallbackRMI;
import com.velox.api.util.ServerException;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDataRecord implements DataRecordProxy {
    // only getDataField & setDataField currently implemented
    HashMap<String, Object> map = new HashMap<>();

    @Override
    public Object getDataField(long recordId, String dataTypeName, String dataFieldName, User user) throws NotFound, RemoteException {
        return map.get(dataFieldName);
    }
    @Override
    public void setDataField(long recordId, String dataTypeName, String dataFieldName, Object newValue, User user) throws IoError, InvalidValue, NotFound, RemoteException {
        map.put(dataFieldName, newValue);
    }

    @Override
    public String getName(long l, String s, User user) throws RemoteException {
        return null;
    }

    @Override
    public boolean hasChildren(long l, String s, User user) throws IoError, RemoteException {
        return false;
    }

    @Override
    public DataRecord[] getChildren(long l, String s, User user) throws IoError, RemoteException {
        return new DataRecord[0];
    }

    @Override
    public DataRecordAttributes getAttributes(long l, String s, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public DataRecordAttributes[] getChildAttributesList(long l, String s, User user) throws IoError, RemoteException {
        return new DataRecordAttributes[0];
    }

    @Override
    public DataRecord addChild(long l, String s, DataFields dataFields, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public DataRecord addChild(long l, String s, String s1, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public void removeChild(long l, String s, DataRecord dataRecord, ClientCallbackRMI clientCallbackRMI, User user) throws IoError, NotFound, RemoteException {

    }

    @Override
    public DataRecord copyDataRecord(long l, String s, DataRecord dataRecord, boolean b, User user) throws IoError, AlreadyExists, InvalidValue, RemoteException {
        return null;
    }

    @Override
    public DataFields getDataFields(long l, String s, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public boolean verifyDataFields(long l, String s, List<String> list, boolean b, ClientCallbackRMI clientCallbackRMI, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public boolean hasAccess(long l, String s, User user, int i) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public void checkAccess(long l, String s, User user, int i) throws ServerException, RemoteException {

    }

    @Override
    public boolean isReadOnly(long l, String s, User user) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public DataRecord[] getChildrenOfType(long l, String s, String s1, User user) throws IoError, RemoteException {
        return new DataRecord[0];
    }

    @Override
    public DataRecord findChild(long l, String s, String s1, String s2, Object o, User user) throws RemoteException {
        return null;
    }

    @Override
    public DataRecord findChild(long l, String s, String s1, String s2, Object o, String s3, Object o1, User user) throws RemoteException {
        return null;
    }

    @Override
    public boolean getBooleanVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return false;
    }

    @Override
    public int getIntegerVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public long getLongVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public double getDoubleVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getEnumVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getShortVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public String getStringVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getFileVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getSelectionVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public long getDateVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public Object getValue(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public boolean getLastSavedBooleanVal(long l, String s, String s1) throws NotFound, RemoteException {
        return false;
    }

    @Override
    public String getLastSavedSelectionVal(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public int getLastSavedIntegerVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public long getLastSavedLongVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public double getLastSavedDoubleVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getLastSavedEnumVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public short getLastSavedShortVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public String getLastSavedStringVal(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getLastSavedFileVal(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public long getLastSavedDateVal(long l, String s, String s1) throws NotFound, RemoteException {
        return 0;
    }

    @Override
    public Object getLastSavedValue(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public Object getLastSavedDataField(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public void setDataFields(long l, String s, DataFields dataFields, User user) throws IoError, InvalidValue, NotFound, RemoteException {

    }

    @Override
    public List<DataRecord> getParentList(long l, String s, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public void delete(long l, String s, ClientCallbackRMI clientCallbackRMI, User user, boolean b) throws IoError, NotFound, RemoteException {

    }

    @Override
    public DataRecord addChild(long l, String s, DataRecord dataRecord, User user) throws AlreadyExists, NotFound, IoError, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> getParentsOfType(long l, String s, String s1, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public boolean isDeleted(long l, String s) throws RemoteException {
        return false;
    }

    @Override
    public boolean[] getChildHasChildrenList(long l, String s, User user) throws IoError, RemoteException {
        return new boolean[0];
    }

    @Override
    public List<DataRecordInfo> getChildResultsOfType(long l, String s, String s1, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public List<DataRecordInfo> getParentResultsOfType(long l, String s, String s1, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public DataRecordInfo getDataRecordInfo(long l, String s, User user) throws RemoteException {
        return null;
    }

    @Override
    public boolean isNew(long l, String s) throws RemoteException {
        return false;
    }

    @Override
    public boolean isChanged(long l, String s, String[] strings) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public boolean isChanged(long l, String s, String s1) throws ServerException, RemoteException {
        return false;
    }

    @Override
    public List<DataRecord> getDescendantsOfType(long l, String s, String s1, User user) throws RemoteException {
        return null;
    }

    @Override
    public List<DataRecordInfo> getDescendantInfoOfType(long l, String s, String s1, User user) throws RemoteException {
        return null;
    }

    @Override
    public DataRecordACL getDataRecordACL(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void setDataRecordACL(long l, String s, DataRecordACL dataRecordACL, boolean b, User user) throws ServerException, RemoteException {

    }

    @Override
    public void revertDataRecordACL(long l, String s, User user) throws ServerException, RemoteException {

    }

    @Override
    public Map<String, Object> getFields(long l, String s, User user) throws RemoteException {
        return null;
    }

    @Override
    public void setFields(long l, String s, Map<String, Object> map, User user) throws ServerException, RemoteException {

    }

    @Override
    public DataRecord addChild(long l, String s, String s1, Map<String, Object> map, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> addChildren(long l, String s, String s1, int i, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecord> addChildren(long l, String s, String s1, List<Map<String, Object>> list, User user) throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        return null;
    }

    @Override
    public void addChildrenDataPump(long l, String s, String s1, List<Map<String, Object>> list, User user) throws ServerException, RemoteException {

    }

    @Override
    public List<DataRecord> getAncestorsOfType(long l, String s, String s1, User user) throws RemoteException {
        return null;
    }

    @Override
    public void addChildren(long l, String s, List<DataRecord> list, User user) throws AlreadyExists, NotFound, IoError, RemoteException {

    }

    @Override
    public String getPickListVal(long l, String s, String s1, User user) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public String getLastSavedPickListVal(long l, String s, String s1) throws NotFound, RemoteException {
        return null;
    }

    @Override
    public DataRecord addChildIfNotExists(long l, String s, DataRecord dataRecord, User user) throws IoError, RemoteException {
        return null;
    }

    @Override
    public boolean hasChild(long l, String s, long l1, User user) throws IoError, RemoteException {
        return false;
    }

    @Override
    public boolean isAttachment(long l, String s) throws RemoteException {
        return false;
    }

    @Override
    public void setAttachmentData(long l, String s, byte[] bytes, User user) throws ServerException, RemoteException {

    }

    @Override
    public byte[] getAttachmentData(long l, String s, User user) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getAttachmentData(long l, String s, User user, int i) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getAttachmentThumbnail(long l, String s, User user) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getAttachmentThumbnail(long l, String s, User user, int i) throws ServerException, RemoteException {
        return new byte[0];
    }

    @Override
    public AttachmentAnnotation getAttachmentAnnotation(long l, String s, int i, User user) throws RemoteException, ServerException {
        return null;
    }

    @Override
    public AttachmentAnnotation getAttachmentAnnotation(long l, String s, User user) throws RemoteException, ServerException {
        return null;
    }

    @Override
    public void setAttachmentAnnotation(long l, String s, AttachmentAnnotation attachmentAnnotation, User user) throws RemoteException, ServerException {

    }

    @Override
    public boolean isAttachmentAnnotationChanged(long l, String s) throws RemoteException {
        return false;
    }

    @Override
    public boolean isAttachmentDataChanged(long l, String s) throws RemoteException {
        return false;
    }

    @Override
    public List<AttachmentVersion> getAttachmentVersionList(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void deleteAttachmentVersion(long l, String s, int i, User user) throws ServerException, RemoteException {

    }

    @Override
    public void checkOut(long l, String s, User user) throws ServerException, RemoteException {

    }

    @Override
    public void cancelCheckOut(long l, String s, User user) throws ServerException, RemoteException {

    }

    @Override
    public void checkIn(long l, String s, User user) throws ServerException, RemoteException {

    }

    @Override
    public void promoteDraft(long l, String s, User user) throws ServerException, RemoteException {

    }

    @Override
    public void requestApproval(long l, String s, String s1, User user) throws ServerException, RemoteException {

    }

    @Override
    public void approve(long l, String s, String s1, User user) throws ServerException, RemoteException {

    }

    @Override
    public void reject(long l, String s, String s1, User user) throws ServerException, RemoteException {

    }

    @Override
    public DataRecordApproval getApproval(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public List<DataRecordApproval> getApprovalList(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public DataRecordComment createComment(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }

    @Override
    public void addComment(long l, String s, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public void deleteComment(long l, String s, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public void editComment(long l, String s, DataRecordComment dataRecordComment, User user) throws ServerException, RemoteException {

    }

    @Override
    public List<DataRecordComment> getCommentList(long l, String s, User user) throws ServerException, RemoteException {
        return null;
    }
}