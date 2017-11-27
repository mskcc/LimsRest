package org.mskcc.limsrest.limsapi.dmp;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.util.TestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DateRetrieverTest {
    private DateRetriever dateRetriever = new DefaultTodayDateRetriever();

    @Test
    public void whenDateIsNull_shouldReturnTodayDate() throws Exception {
        LocalDate date = dateRetriever.retrieve(null);

        assertThat(date.getDayOfMonth(), is(LocalDateTime.now().getDayOfMonth()));
        assertThat(date.getMonth(), is(LocalDateTime.now().getMonth()));
        assertThat(date.getYear(), is(LocalDateTime.now().getYear()));
    }

    @Test
    public void whenDateIsEmpty_shouldReturnTodayDate() throws Exception {
        LocalDate date = dateRetriever.retrieve("");

        assertThat(date.getDayOfMonth(), is(LocalDateTime.now().getDayOfMonth()));
        assertThat(date.getMonth(), is(LocalDateTime.now().getMonth()));
        assertThat(date.getYear(), is(LocalDateTime.now().getYear()));
    }

    @Test
    public void whenDateHasIncorrectFormat_shouldThrowAnException() throws Exception {
        assertExceptionIsThrown("2017-11-10");
        assertExceptionIsThrown("13-11-2017");
        assertExceptionIsThrown("11-2016-09");
        assertExceptionIsThrown("03/04/1956");
        assertExceptionIsThrown("13-10-2006");
    }

    private void assertExceptionIsThrown(String wrongDate) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> {
            dateRetriever.retrieve(wrongDate);
        });

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(DateTimeParseException.class));
    }

}