package org.mskcc.limsrest.limsapi;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mskcc.util.VeloxConstants;
import org.mskcc.util.email.EmailNotificator;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class GetPickListTest {
    public static final List<String> recipes = Arrays.asList("abc", "kot", "lama", "dede");
    private EmailNotificator emailNotificator = mock(EmailNotificator.class);

    @Test
    public void whenNewRecipesAreAdded_shouldNotifyAboutThem() throws Exception {
        //given
        GetPickList getPickList = new GetPickList((pickListName, dataMgmtServer, user) -> recipes, emailNotificator,
                Arrays.asList("abc", "lama"));

        getPickList.init(VeloxConstants.RECIPE, true);

        //when
        List<String> pickList = (List<String>) getPickList.execute(mock(VeloxConnection.class));

        //then
        Assertions.assertThat(pickList).containsAll(recipes);
        verify(emailNotificator, times(1)).notifyMessage(VeloxConstants.RECIPE, "kot,dede");
    }

    @Test
    public void whenCurrentAndNewRecipeListIsEmpty_shouldNotNotify() throws Exception {
        //given
        GetPickList getPickList = new GetPickList((pickListName, dataMgmtServer, user) -> Lists.emptyList(),
                emailNotificator, Lists.emptyList());
        getPickList.init(VeloxConstants.RECIPE, true);

        //when
        List<String> pickList = (List<String>) getPickList.execute(mock(VeloxConnection.class));

        //then
        Assertions.assertThat(pickList).isEmpty();
        verifyNoMoreInteractions(emailNotificator);
    }

    @Test
    public void whenCurrentAndNewRecipeListIsSame_shouldNotNotify() throws Exception {
        //given
        GetPickList getPickList = new GetPickList((pickListName, dataMgmtServer, user) -> recipes,
                emailNotificator, recipes);
        getPickList.init(VeloxConstants.RECIPE, true);

        //when
        List<String> pickList = (List<String>) getPickList.execute(mock(VeloxConnection.class));

        //then
        Assertions.assertThat(pickList).containsAll(recipes);
        verifyNoMoreInteractions(emailNotificator);
    }
}