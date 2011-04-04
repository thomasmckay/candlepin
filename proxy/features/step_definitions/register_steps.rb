require 'spec/expectations'
require 'candlepin_api'

Given /^I am a consumer "([^\"]*)"$/ do |consumer_name|
  Given "I am a consumer \"#{consumer_name}\" registered by \"#{@username}\""
end

Given /^I am a consumer "([^\"]*)" registered by "([^\"]*)"$/ do |consumer_name, user_name|
  Given "I am logged in as \"#{user_name}\""
  When "I register a consumer \"#{consumer_name}\""
end

Given /^I register a consumer "([^\"]*)" with the following facts:$/ do |consumer_name, facts_table|
  # facts_table is a Cucumber::Ast::Table
  facts = facts_table.rows_hash.delete_if { |key, val| key == 'Name' }
  set_consumer(@current_owner_cp.register(consumer_name, :system, nil, facts))
end

Given /^I am a consumer "([^\"]*)" of type "([^\"]*)" with facts:$/ do |consumer_name, consumer_type, facts_table|
  Given "I am logged in as \"#{@username}\""
  facts = facts_table.rows_hash.delete_if { |key, val| key == 'Name' }
  set_consumer(@current_owner_cp.register(consumer_name, consumer_type, nil, facts))
end


def set_consumer(created_consumer)
    @consumer = created_consumer
    @x509_cert = OpenSSL::X509::Certificate.new(@consumer['idCert']['cert'])
    @cert_extensions = {}
    @x509_cert.extensions.each do |ext|
        @cert_extensions[ext.oid] = ext.value
    end
    @consumer_cp = connect(username=nil, password=nil,
                           cert=@consumer['idCert']['cert'],
                           key=@consumer['idCert']['key'])
    @consumer_cp.consumer = @consumer
    @consumers[created_consumer['name']] = @consumer_cp
end

When /I register a consumer "(\w+)"$/ do |consumer_name|
  set_consumer(@current_owner_cp.register(consumer_name))
end

When /I register a consumer "([^\"]*)" with uuid "([^\"]*)"$/ do |consumer_name, uuid|
  set_consumer(@current_owner_cp.register(consumer_name, :system, uuid))
end

When /^I register a personal consumer$/ do
  set_consumer(@current_owner_cp.register('test', :person))
end

Given /^I have registered a personal consumer with uuid "([^\"]*)"$/ do |uuid|
  set_consumer(@current_owner_cp.register('test', :person, uuid))
end

Given /^I am a consumer "([^\"]*)" of type "([^\"]*)"$/ do |consumer_name, type|
  # This will register with the user you are logged in as
  Given "I am logged in as \"#{@username}\""

  set_consumer(@current_owner_cp.register(consumer_name, type))
end


Given /^Consumer "([^\"]*)" exists with uuid "([^\"]*)"$/ do |consumer_name, uuid|
    When "I register a consumer \"#{consumer_name}\" with uuid \"#{uuid}\""
end

Then /^registering another consumer with uuid "([^\"]*)" causes a bad request$/ do |uuid|

    begin
      @candlepin.register('any name', uuid=uuid)
    rescue RestClient::Exception => e
        e.http_code.should == 400
    else
        assert(fail, "Excepted exception was not raised")
    end

end

Then /^I should not be able to register a new personal consumer$/ do
  begin
    When "I register a personal consumer"
  rescue RestClient::Exception => e
    e.http_code.should == 400   # I think this is the wrong error code
  else
    assert(fail, "Excepted exception was not raised")
  end

end

Then /^searching for a consumer with uuid "([^\"]*)" causes a not found$/ do |uuid|

    lambda {@candlepin.get_consumer(uuid)}.should raise_error
    begin
        @candlepin.get_consumer(uuid)
    rescue RestClient::Exception => e
        e.http_code.should == 404
    end

end

When /I revoke all my entitlements/ do
    @consumer_cp.revoke_all_entitlements
end

Then /^my consumer should have an identity certificate$/ do
    @consumer['idCert']['cert'][0, 3].should eql('---')
    @consumer['idCert']['key'][0, 3].should eql('---')
end

Then /the "([^\"]*)" on my identity certificate's subject is my consumer's UUID/ do |subject_property|
    uuid = @consumer['uuid']
    subject_value(@x509_cert, subject_property).should == uuid
end

Then /the consumers name in the certificate is "([^\"]*)"$/ do |consumer_name|
    altName = @cert_extensions["subjectAltName"]
    altName.gsub!("DirName:/CN=", "")
    altName.should == consumer_name
end

Then /the "([^\"]*)" on my identity certificate's subject is "([^\"]*)"$/ do |subject_property, expected|
    subject_value(@x509_cert, subject_property).should == expected
end

# Grabs the value of a key=value pair in the identity cert's subject
def subject_value(x509_cert, key)
    subject = x509_cert.subject
    subject.to_s.scan(/\/#{key}=([^\/=]+)/)[0][0]
end