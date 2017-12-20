package com.cabolabs.ehrserver

import grails.test.spock.IntegrationSpec
import com.cabolabs.ehrserver.query.*
import com.cabolabs.security.*
import com.cabolabs.ehrserver.ehr.clinical_documents.*

class ResourceServiceIntegrationSpec extends IntegrationSpec {

   def resourceService

   private String ehrUid = '11111111-1111-1111-1111-111111111123'
   private String patientUid = '11111111-1111-1111-1111-111111111145'
   private String orgUid = '11111111-1111-1111-1111-111111111178'
   private String optUid = '47f1ce25-2050-479f-a6a6-3f4700428b1a'
   private String queryUid = '191138e0-24e8-46d5-8789-ca2fde09a321'
   
   private createAdmin()
   {
      def user = new User(
         username: 'testadmin', password: 'testadmin',
         email: 'testadmin@domain.com',
         organizations: [Organization.findByUid(orgUid)]).save(failOnError:true, flush: true)
      
      def adminRole = new Role(authority: Role.AD).save(failOnError: true, flush: true)
      
      UserRole.create( user, adminRole, Organization.findByUid(orgUid), true )
   }
   
   private createOrganization()
   {
      def org = new Organization(uid: orgUid, name: 'CaboLabs', number: '123456')
      org.save(failOnError: true)
   }
   
   private createQuery()
   {
      def user = User.findByUsername("testadmin")
      
      def q = new Query(name:'query', type:'datavalue',
                        isPublic:true, format:'json',
                        group:'path', uid: queryUid,
                        author: user,
                        organizationUid: orgUid)
                        
      q.select = [
         new DataGet(archetypeId: 'openEHR-EHR-OBSERVATION.blood_pressure.v1', 
                     path:        '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value', 
                     rmTypeName:  'DV_QUANTITY',
                     allowAnyArchetypeVersion: false),
         new DataGet(archetypeId: 'openEHR-EHR-OBSERVATION.blood_pressure.v1', 
                     path:        '/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value', 
                     rmTypeName:  'DV_QUANTITY',
                     allowAnyArchetypeVersion: false)
      ]
      
      q.save(failOnError: true)
   }
   
   private createOperationalTemplateIndex()
   {
      def opt = new OperationalTemplateIndex(
         templateId: 'simple_encounter_en.v1', 
         concept: 'simple_encounter_en.v1',
         language: 'ISO_639-1::en',
         external_uid: optUid,
         archetypeId: 'openEHR-EHR-COMPOSITION.encounter.v1',
         archetypeConcept: 'encounter',
         isPublic: false,
         organizationUid: orgUid
      )
      
      opt.save(failOnError: true)
   }

   def setup()
   {
      createOrganization()
      createAdmin()
      
      createQuery()
      createOperationalTemplateIndex()
   }

   def cleanup()
   {
      def query = Query.findByUid(queryUid)
      
      def shares = QueryShare.findAllByQuery(query)
      shares.each { share ->
         share?.delete(failOnError: true)
      }
      
      query.delete()
      
      
      def user = User.findByUsername("testadmin")
      def role = Role.findByAuthority(Role.AD)
      def org = Organization.findByUid(orgUid)
      
      UserRole.remove(user, role, org)
      user.delete(flush: true)
      
      org.delete()

      
      def opt = OperationalTemplateIndex.findByUid(optUid)
      opt.delete()
   }

   void "shareQuery"()
   {
      when:
         def query = Query.findByUid(queryUid)
         def org = Organization.findByUid(orgUid)
         resourceService.shareQuery(query, org)
         def shares = QueryShare.list()
      then:
         assert shares.size() == 1
         assert shares[0].query.uid == queryUid
         assert shares[0].organization.uid == orgUid
   }
   
   void "cleanSharesQuery"()
   {
      when:
         def query = Query.findByUid(queryUid)
         def org = Organization.findByUid(orgUid)
         resourceService.shareQuery(query, org)
         def shares1 = QueryShare.list()
         
         resourceService.cleanSharesQuery(query)
         def shares2 = QueryShare.list()
      then:
         assert shares1.size() == 1
         assert shares2.size() == 0
   }
   
   /*
   void "shareOpt"()
   {
      when:
         def opt = OperationalTemplateIndex.findByUid(optUid)
         def org = Organization.findByUid(orgUid)
         resourceService.shareOpt(opt, org)
         def shares = OperationalTemplateIndexShare.list()
      then:
         assert shares.size() == 1
         assert shares[0].opt.uid == optUid
         assert shares[0].organization.uid == orgUid
   }
   
   void "cleanSharesOpt"()
   {
      when:
         def opt = OperationalTemplateIndex.findByUid(optUid)
         def org = Organization.findByUid(orgUid)
         resourceService.shareOpt(opt, org)
         def shares1 = OperationalTemplateIndexShare.list()
         
         resourceService.cleanSharesOpt(opt)
         def shares2 = OperationalTemplateIndexShare.list()
      then:
         assert shares1.size() == 1
         assert shares2.size() == 0
   }
   */
}
