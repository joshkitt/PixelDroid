package com.h.pixeldroid.utils.api

import com.h.pixeldroid.utils.api.objects.*
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Field

/*
    Implements the Pixelfed API
    https://docs.pixelfed.org/technical-documentation/api-v1.html
    However, since this is mostly based on the Mastodon API, the documentation there
    will be more useful: https://docs.joinmastodon.org/
 */

interface PixelfedAPI {


    companion object {
        fun createFromUrl(baseUrl: String): PixelfedAPI {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(PixelfedAPI::class.java)
        }
    }


    @FormUrlEncoded
    @POST("/api/v1/apps")
    suspend fun registerApplication(
        @Field("client_name") client_name: String,
        @Field("redirect_uris") redirect_uris: String,
        @Field("scopes") scopes: String? = null,
        @Field("website") website: String? = null
    ): Application


    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun obtainToken(
        @Field("client_id") client_id: String,
        @Field("client_secret") client_secret: String,
        @Field("redirect_uri") redirect_uri: String? = null,
        @Field("scope") scope: String? = "read",
        @Field("code") code: String? = null,
        @Field("grant_type") grant_type: String? = null,
        @Field("refresh_token") refresh_token: String? = null
    ): Token

    // get instance configuration
    @GET("/api/v1/instance")
    suspend fun instance() : Instance

    /**
     * Instance info from the Nodeinfo .well_known (https://nodeinfo.diaspora.software/protocol.html) endpoint
     */
    @GET("/.well-known/nodeinfo")
    suspend fun wellKnownNodeInfo() : NodeInfoJRD

    /**
     * Instance info from [NodeInfo] (https://nodeinfo.diaspora.software/schema.html) endpoint
     */
    @GET
    suspend fun nodeInfoSchema(
            @Url nodeInfo_schema_url: String
    ) : NodeInfo

    @FormUrlEncoded
    @POST("/api/v1/accounts/{id}/follow")
    suspend fun follow(
        //The authorization header needs to be of the form "Bearer <token>"
        @Path("id") statusId: String,
        @Header("Authorization") authorization: String,
        @Field("reblogs") reblogs : Boolean = true
    ) : Relationship

    @POST("/api/v1/accounts/{id}/unfollow")
    suspend fun unfollow(
        //The authorization header needs to be of the form "Bearer <token>"
        @Path("id") statusId: String,
        @Header("Authorization") authorization: String
    ) : Relationship

    @POST("api/v1/statuses/{id}/favourite")
    suspend fun likePost(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Path("id") statusId: String

    ) : Status

    @POST("/api/v1/statuses/{id}/unfavourite")
    suspend fun unlikePost(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Path("id") statusId: String
    ) : Status

    //Used in our case to post a comment or a status
    @FormUrlEncoded
    @POST("/api/v1/statuses")
    suspend fun postStatus(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Field("status") statusText : String,
        @Field("in_reply_to_id") in_reply_to_id : String? = null,
        @Field("media_ids[]") media_ids : List<String> = emptyList(),
        @Field("poll[options][]") poll_options : List<String>? = null,
        @Field("poll[expires_in]") poll_expires : List<String>? = null,
        @Field("poll[multiple]") poll_multiple : List<String>? = null,
        @Field("poll[hide_totals]") poll_hideTotals : List<String>? = null,
        @Field("sensitive") sensitive : Boolean? = null,
        @Field("spoiler_text") spoiler_text : String? = null,
        @Field("visibility") visibility : String = "public",
        @Field("scheduled_at") scheduled_at : String? = null,
        @Field("language") language : String? = null
    ) : Status

    @DELETE("/api/v1/statuses/{id}")
    suspend fun deleteStatus(
            @Header("Authorization") authorization: String,
            @Path("id") statusId: String
    )

    @FormUrlEncoded
    @POST("/api/v1/statuses/{id}/reblog")
    suspend fun reblogStatus(
        @Header("Authorization") authorization: String,
        @Path("id") statusId: String,
        @Field("visibility") visibility: String? = null
    ) : Status

    @POST("/api/v1/statuses/{id}/unreblog")
    suspend fun undoReblogStatus(
        @Path("id") statusId: String,
        @Header("Authorization") authorization: String
    ) : Status

    //Used in our case to retrieve comments for a given status
    @GET("/api/v1/statuses/{id}/context")
    suspend fun statusComments(
        @Path("id") statusId: String,
        @Header("Authorization") authorization: String? = null
    ) : Context

    @GET("/api/v1/timelines/public")
    suspend fun timelinePublic(
        @Query("local") local: Boolean? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null
    ): List<Status>

    @GET("/api/v1/timelines/home")
    suspend fun timelineHome(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null,
        @Query("local") local: Boolean? = null
    ): List<Status>

    @GET("/api/v2/search")
    suspend fun search(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Query("account_id") account_id: String? = null,
        @Query("max_id") max_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("type") type: Results.SearchType? = null,
        @Query("exclude_unreviewed") exclude_unreviewed: Boolean? = null,
        @Query("q") q: String,
        @Query("resolve") resolve: Boolean? = null,
        @Query("limit") limit: String? = null,
        @Query("offset") offset: String? = null,
        @Query("following") following: Boolean? = null
    ): Results

    @GET("/api/v1/notifications")
    suspend fun notifications(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null,
        @Query("exclude_types") exclude_types: List<String>? = null,
        @Query("account_id") account_id: Boolean? = null
    ): List<Notification>

    @GET("/api/v1/accounts/verify_credentials")
    suspend fun verifyCredentials(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String
    ): Account


    @GET("/api/v1/accounts/{id}/statuses")
    fun accountPosts(
        @Header("Authorization") authorization: String,
        @Path("id") account_id: String? = null
    ): Call<List<Status>>

    @GET("/api/v1/accounts/relationships")
    suspend fun checkRelationships(
        @Header("Authorization") authorization : String,
        @Query("id[]") account_ids : List<String>
    ) : List<Relationship>

    @GET("/api/v1/accounts/{id}/followers")
    suspend fun followers(
        @Path("id") account_id: String,
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("limit") limit: Number? = null,
        @Query("page") page: String? = null
    ) : Response<List<Account>>

    @GET("/api/v1/accounts/{id}/following")
    suspend fun following(
        @Path("id") account_id: String,
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("limit") limit: Number? = 40,
        @Query("page") page: String? = null
    ) : Response<List<Account>>

    @GET("/api/v1/accounts/{id}")
    suspend fun getAccount(
        @Header("Authorization") authorization: String,
        @Path("id") accountId : String
    ): Account

    @GET("/api/v1/statuses/{id}")
    suspend fun getStatus(
        @Header("Authorization") authorization: String,
        @Path("id") accountId : String
    ): Status

    @Multipart
    @POST("/api/v1/media")
    fun mediaUpload(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Part description: MultipartBody.Part? = null,
        @Part file: MultipartBody.Part
    ): Observable<Attachment>

    // get discover
    @GET("/api/v2/discover/posts")
    suspend fun discover(
        @Header("Authorization") authorization: String
    ) : DiscoverPosts

    @FormUrlEncoded
    @POST("/api/v1/reports")
    @JvmSuppressWildcards
    suspend fun report(
        @Header("Authorization") authorization: String,
        @Field("account_id") account_id: String,
        @Field("status_ids") status_ids: List<Status>,
        @Field("comment") comment: String,
        @Field("forward") forward: Boolean = true
    ) : Report

}