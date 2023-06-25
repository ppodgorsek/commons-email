/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.mail.mocks.MockHtmlEmailConcrete;
import org.apache.commons.mail.settings.EmailConfiguration;
import org.apache.commons.mail.util.MimeMessageParser;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.internet.MimeMessage;

/**
 * JUnit test case for HtmlEmail Class.
 *
 * @since 1.0
 */
public class HtmlEmailTest extends AbstractEmailTest
{

	private MockHtmlEmailConcrete email;

    @BeforeEach
    public void setUpHtmlEmailTest()
    {
        // reusable objects to be used across multiple tests
        this.email = new MockHtmlEmailConcrete();
    }

    @Test
    public void testGetSetTextMsg() throws EmailException
    {
        // ====================================================================
        // Test Success
        // ====================================================================
        for (final String validChar : testCharsValid)
        {
            this.email.setTextMsg(validChar);
            assertEquals(validChar, this.email.getTextMsg());
        }

        // ====================================================================
        // Test Exception
        // ====================================================================
        for (final String invalidChar : this.testCharsNotValid)
        {
            try
            {
                this.email.setTextMsg(invalidChar);
                fail("Should have thrown an exception");
            }
            catch (final EmailException e)
            {
                assertTrue(true);
            }
        }

    }

    @Test
    public void testGetSetHtmlMsg() throws EmailException
    {
        // ====================================================================
        // Test Success
        // ====================================================================
        for (final String validChar : testCharsValid)
        {
            this.email.setHtmlMsg(validChar);
            assertEquals(validChar, this.email.getHtmlMsg());
        }

        // ====================================================================
        // Test Exception
        // ====================================================================
        for (final String invalidChar : this.testCharsNotValid)
        {
            try
            {
                this.email.setHtmlMsg(invalidChar);
                fail("Should have thrown an exception");
            }
            catch (final EmailException e)
            {
                assertTrue(true);
            }
        }

    }

    @Test
    public void testGetSetMsg() throws EmailException
    {
        // ====================================================================
        // Test Success
        // ====================================================================
        for (final String validChar : testCharsValid)
        {
            this.email.setMsg(validChar);
            assertEquals(validChar, this.email.getTextMsg());

            assertTrue(
                    this.email.getHtmlMsg().contains(validChar));
        }

        // ====================================================================
        // Test Exception
        // ====================================================================
        for (final String invalidChar : this.testCharsNotValid)
        {
            try
            {
                this.email.setMsg(invalidChar);
                fail("Should have thrown an exception");
            }
            catch (final EmailException e)
            {
                assertTrue(true);
            }
        }

    }

    @Test
    public void testEmbedUrl() throws Exception
    {
        // ====================================================================
        // Test Success
        // ====================================================================

        final String strEmbed =
            this.email.embed(new URL(this.strTestURL), "Test name");
        assertNotNull(strEmbed);
        assertEquals(HtmlEmail.CID_LENGTH, strEmbed.length());

        // if we embed the same name again, do we get the same content ID
        // back?
        final String testCid =
            this.email.embed(new URL(this.strTestURL), "Test name");
        assertEquals(strEmbed, testCid);

        // if we embed the same URL under a different name, is the content ID
        // unique?
        final String newCid =
            this.email.embed(new URL(this.strTestURL), "Test name 2");
        assertFalse(strEmbed.equals(newCid));

        // ====================================================================
        // Test Exceptions
        // ====================================================================

        // Does an invalid URL throw an exception?
        // Create a mocked URL object which always throws an IOException
        // when the openStream() method is called.
        //
        // Several ISPs do resolve invalid URLs like {@code http://example.invalid}
        // to some error page causing tests to fail otherwise.

        MockURLStreamHandler handler = EasyMock.createStrictMock(MockURLStreamHandler.class);
        final URL url = new URL("http", "example.invalid", 8888, "invalid.html", handler);

        EasyMock.expect(handler.openConnection(EasyMock.anyObject(URL.class))).andThrow(new IOException());
        EasyMock.replay(handler);

        try
        {
            this.email.embed(url, "Bad URL");
            fail("Should have thrown an exception");
        }
        catch (final EmailException e)
        {
            // expected
        }

        EasyMock.verify(handler);

        // if we try to embed a different URL under a previously used name,
        // does it complain?
        try
        {
            this.email.embed(new URL("http://www.google.com"), "Test name");
            fail("shouldn't be able to use an existing name with a different URL!");
        }
        catch (final EmailException e)
        {
            // expected
        }
    }

    @Test
    public void testEmbedFile() throws Exception
    {
        // ====================================================================
        // Test Success
        // ====================================================================

        final File file = File.createTempFile("testEmbedFile", "txt");
        file.deleteOnExit();
        final String strEmbed = this.email.embed(file);
        assertNotNull(strEmbed);
        assertEquals(HtmlEmail.CID_LENGTH, strEmbed.length(),
        		"generated CID has wrong length");

        // if we embed the same file again, do we get the same content ID
        // back?
        final String testCid =
            this.email.embed(file);
        assertEquals(strEmbed, testCid,
        		"didn't get same CID after embedding same file twice");

        // if we embed a new file, is the content ID unique?
        final File otherFile = File.createTempFile("testEmbedFile2", "txt");
        otherFile.deleteOnExit();
        final String newCid = this.email.embed(otherFile);
        assertFalse(strEmbed.equals(newCid),
        		"didn't get unique CID from embedding new file");
    }

    @Test
    public void testEmbedUrlAndFile() throws Exception
    {
        final File tmpFile = File.createTempFile("testfile", "txt");
        tmpFile.deleteOnExit();
        final String fileCid = this.email.embed(tmpFile);

        final URL fileUrl = tmpFile.toURI().toURL();
        final String urlCid = this.email.embed(fileUrl, "urlName");

        assertFalse(fileCid.equals(urlCid),
        		"file and URL cids should be different even for same resource");
    }

    @Test
    public void testEmbedDataSource() throws Exception
    {
        final File tmpFile = File.createTempFile("testEmbedDataSource", "txt");
        tmpFile.deleteOnExit();
        final FileDataSource dataSource = new FileDataSource(tmpFile);

        // does embedding a datasource without a name fail?
        try
        {
            this.email.embed(dataSource, "");
            fail("embedding with an empty string for a name should fail");
        }
        catch (final EmailException e)
        {
            // expected
        }

        // properly embed the datasource
        final String cid = this.email.embed(dataSource, "testname");

        // does embedding the same datasource under the same name return
        // the original cid?
        final String sameCid = this.email.embed(dataSource, "testname");
        assertEquals(cid, sameCid,
        		"didn't get same CID for embedding same datasource twice");

        // does embedding another datasource under the same name fail?
        final File anotherFile = File.createTempFile("testEmbedDataSource2", "txt");
        anotherFile.deleteOnExit();
        final FileDataSource anotherDS = new FileDataSource(anotherFile);
        try
        {
            this.email.embed(anotherDS, "testname");
        }
        catch (final EmailException e)
        {
            // expected
        }
    }

    /**
     * @throws EmailException when bad addresses and attachments are used
     * @throws IOException if creating a temp file, URL or sending fails
     */
    @Test
    public void testSend() throws EmailException, IOException
    {
        final EmailAttachment attachment = new EmailAttachment();

        /** File to used to test file attachments (Must be valid) */
        final File testFile = File.createTempFile("commons-email-testfile", ".txt");
        testFile.deleteOnExit();

        // ====================================================================
        // Test Success
        // ====================================================================
        this.getMailServer();

        String strSubject = "Test HTML Send #1 Subject (w charset)";

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);

        /** File to used to test file attachmetns (Must be valid) */
        attachment.setName("Test Attachment");
        attachment.setDescription("Test Attachment Desc");
        attachment.setPath(testFile.getAbsolutePath());
        this.email.attach(attachment);

        //this.email.setAuthentication(this.strTestUser, this.strTestPasswd);

        this.email.setCharset(EmailConstants.ISO_8859_1);
        this.email.setSubject(strSubject);

        final URL url = new URL(EmailConfiguration.TEST_URL);
        final String cid = this.email.embed(url, "Apache Logo");

        final String strHtmlMsg =
            "<html>The Apache logo - <img src=\"cid:" + cid + "\"><html>";

        this.email.setHtmlMsg(strHtmlMsg);
        this.email.setTextMsg(
            "Your email client does not support HTML emails");

        this.email.send();
        this.fakeMailServer.stop();
        // validate txt message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getTextMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);

        // validate html message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getHtmlMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            false);

        // validate attachment
        validateSend(
            this.fakeMailServer,
            strSubject,
            attachment.getName(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            false);

        this.getMailServer();

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        strSubject = "Test HTML Send #1 Subject (wo charset)";
        this.email.setSubject(strSubject);
        this.email.setTextMsg("Test message");

        this.email.send();
        this.fakeMailServer.stop();
        // validate txt message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getTextMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);
    }

    @Test
    public void testSend2() throws Exception
    {
        // ====================================================================
        // Test Success
        // ====================================================================

        this.getMailServer();

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        String strSubject = "Test HTML Send #2 Subject (wo charset)";
        this.email.setSubject(strSubject);
        this.email.setMsg("Test txt msg");

        this.email.send();
        this.fakeMailServer.stop();
        // validate txt message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getTextMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);

        // validate html message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getHtmlMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            false);

        this.getMailServer();

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setFrom(this.strTestMailFrom);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.addTo(this.strTestMailTo);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        strSubject = "Test HTML Send #2 Subject (w charset)";
        this.email.setCharset(EmailConstants.ISO_8859_1);
        this.email.setSubject(strSubject);
        this.email.setMsg("Test txt msg");

        this.email.send();
        this.fakeMailServer.stop();
        // validate txt message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getTextMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);

        // validate html message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getHtmlMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            false);

    }

    @Test
    @Disabled
    public void testSendWithDefaultCharset() throws Exception
    {
        // Test is disabled as its result is dependent on the execution order:
        // the mail.mime.charset property is normally cached by the MimeUtility
        // class, thus setting it to another value while running the tests
        // might not have the expected result.

        // ====================================================================
        // Test Success
        // ====================================================================

        System.setProperty(EmailConstants.MAIL_MIME_CHARSET, "iso-8859-15");

        this.getMailServer();

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        final String strSubject = "Test HTML Send Subject (w default charset)";
        this.email.setSubject(strSubject);
        this.email.setMsg("Test txt msg ä"); // add non-ascii character, otherwise us-ascii will be used

        this.email.send();
        this.fakeMailServer.stop();
        // validate charset
        validateSend(
            this.fakeMailServer,
            strSubject,
            "charset=iso-8859-15",
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);

        System.clearProperty(EmailConstants.MAIL_MIME_CHARSET);
    }

    /**
     * Create a HTML email containing an URL pointing to a ZIP file
     * to be downloaded. According to EMAIL-93 the resulting URL
     * "http://paradisedelivery.homeip.net/delivery/?file=TZC268X93337..zip"
     * contains TWO dots instead of one dot which breaks the link.
     */
    @Test
    public void testAddZipUrl() throws Exception
    {
        final String htmlMsg =
                "Please click on the following link: <br><br>" +
                "<a href=\"http://paradisedelivery.homeip.net/delivery/?file=3DTZC268X93337.zip\">" +
                "http://paradisedelivery.homeip.net/delivery/?file=3DTZC268X93337.zip" +
                "</a><br><br>Customer satisfaction is very important for us.";

        this.getMailServer();

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);
        this.email.setCharset(EmailConstants.ISO_8859_1);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        final String strSubject = "A dot (\".\") is appended to some ULRs of a HTML mail.";
        this.email.setSubject(strSubject);
        this.email.setHtmlMsg(htmlMsg);

        this.email.send();
        this.fakeMailServer.stop();

        // validate html message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getHtmlMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            false);

        // make sure that no double dots show up
        assertTrue(this.email.getHtmlMsg().contains("3DTZC268X93337.zip"));
        assertFalse(this.email.getHtmlMsg().contains("3DTZC268X93337..zip"));
    }

    /**
     * According to EMAIL-95 calling buildMimeMessage() before calling send()
     * causes duplicate mime parts - now we throw an exception to catch the
     * problem
     */
    @Test
    public void testCallingBuildMimeMessageBeforeSent() throws Exception {

        final String htmlMsg = "<b>Hello World</b>";

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);
        this.email.setCharset(EmailConstants.ISO_8859_1);

        if (this.strTestUser != null && this.strTestPasswd != null)
        {
            this.email.setAuthentication(
                this.strTestUser,
                this.strTestPasswd);
        }

        final String strSubject = "testCallingBuildMimeMessageBeforeSent";
        this.email.setSubject(strSubject);
        this.email.setHtmlMsg(htmlMsg);

        // this should NOT be called when sending a message
        this.email.buildMimeMessage();

        try
        {
            this.email.send();
        }
        catch (final IllegalStateException e)
        {
            return;
        }

        fail("Expecting an exception when calling buildMimeMessage() before send() ...");
    }

    /**
     * EMAIL-73 - check that providing a plain text content using setMsg()
     * creates a plain content and HTML content using {@code <pre>} tags.
     */
    @Test
    public void testSendWithPlainTextButNoHtmlContent() throws EmailException, IOException
    {
        this.getMailServer();

        final String strSubject = "testSendWithPlainTextButNoHtmlContent";

        this.email = new MockHtmlEmailConcrete();
        this.email.setHostName(this.strTestMailServer);
        this.email.setSmtpPort(this.getMailServerPort());
        this.email.setFrom(this.strTestMailFrom);
        this.email.addTo(this.strTestMailTo);
        this.email.setAuthentication(this.strTestUser, this.strTestPasswd);
        this.email.setCharset(EmailConstants.ISO_8859_1);
        this.email.setSubject(strSubject);
        this.email.setMsg("This is a plain text content : <b><&npsb;></html></b>");

        this.email.send();

        this.fakeMailServer.stop();

        // validate text message
        validateSend(
            this.fakeMailServer,
            strSubject,
            this.email.getTextMsg(),
            this.email.getFromAddress(),
            this.email.getToAddresses(),
            this.email.getCcAddresses(),
            this.email.getBccAddresses(),
            true);
    }

    /**
     * Test that the specified Content-ID is used when embedding a File
     * object in an HtmlEmail.
     *
     * Rolled back the changes since they broke real emails therefore
     * the test is currently disabled.
     *
     * see https://issues.apache.org/jira/browse/EMAIL-101
     */
    @Test
    public void testEmbedFileWithCID() throws Exception
    {
         // ====================================================================
         // Test Success
         // ====================================================================

         final File file = File.createTempFile("testEmbedFile", "txt");
         file.deleteOnExit();

         final String testCid = "Test CID";
         final String encodedCid = EmailUtils.encodeUrl(testCid);

         // if we embed a new file, do we get the content ID we specified back?
         final String strEmbed = this.email.embed(file, testCid);
         assertNotNull(strEmbed);
         assertEquals(encodedCid, strEmbed, "didn't get same CID when embedding with a specified CID");

         // if we embed the same file again, do we get the same content ID
         // back?
         final String returnedCid = this.email.embed(file);
         assertEquals(encodedCid, returnedCid, "didn't get same CID after embedding same file twice");
    }

    @Test
    public void testHtmlMailMimeLayout() throws Exception
    {
        assertCorrectContentType("contentTypeTest.gif", "image/gif");
        assertCorrectContentType("contentTypeTest.jpg", "image/jpeg");
        assertCorrectContentType("contentTypeTest.png", "image/png");
    }

    private void assertCorrectContentType(final String picture, final String contentType) throws Exception {
        final HtmlEmail htmlEmail = createDefaultHtmlEmail();
        final String cid = htmlEmail.embed(new File("./src/test/resources/images/" + picture), "Apache Logo");
        final String htmlMsg = "<html><img src=\"cid:" + cid + "\"><html>";
        htmlEmail.setHtmlMsg(htmlMsg);
        htmlEmail.buildMimeMessage();

        final MimeMessage mm = htmlEmail.getMimeMessage();
        mm.saveChanges();
        final MimeMessageParser mmp = new MimeMessageParser(mm);
        mmp.parse();

        final List<?> attachments = mmp.getAttachmentList();
        assertEquals(1, attachments.size(), "Attachment size");

        final DataSource ds = (DataSource) attachments.get(0);
        assertEquals(contentType, ds.getContentType(), "Content type");
    }

    private HtmlEmail createDefaultHtmlEmail() throws EmailException {
        final HtmlEmail htmlEmail = new HtmlEmail();
        htmlEmail.setHostName(this.strTestMailServer);
        htmlEmail.setSmtpPort(this.getMailServerPort());
        htmlEmail.setFrom("a@b.com");
        htmlEmail.addTo("c@d.com");
        return htmlEmail;
    }
}
