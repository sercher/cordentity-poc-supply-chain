/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.luxoft.supplychain.sovrinagentapp.di

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.blockchainlab.hyperledger.indy.WalletConfig
import com.luxoft.blockchainlab.hyperledger.indy.utils.PoolManager
import com.luxoft.blockchainlab.hyperledger.indy.utils.SerializationUtils
import com.luxoft.supplychain.sovrinagentapp.communcations.SovrinAgentService
import com.luxoft.supplychain.sovrinagentapp.indy.DummyIndyAgentService
import com.luxoft.supplychain.sovrinagentapp.indy.IndyAgentService
import com.luxoft.supplychain.sovrinagentapp.ui.GENESIS_PATH
import io.realm.RealmObject
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet
import org.hyperledger.indy.sdk.wallet.WalletExistsException
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit.GsonConverterFactory
import retrofit.Retrofit
import retrofit.RxJavaCallAdapterFactory
import java.io.File
import java.util.*
import kotlin.math.absoluteValue


// Koin module
val myModule: Module = module {
    single { provideGson() }
    single { provideApiClient(get()) } // get() will resolve Service instance
    factory { provideWalletAndPool() }
    factory { provideIndyUser(get()) }
    single { provideIndyService(get(), get()) }
}

fun provideIndyService(you: IndyUser, counterParty: IndyUser): IndyAgentService = DummyIndyAgentService(you, counterParty)

fun provideWalletAndPool(): Pair<Wallet, Pool> {
    val walletConfig = SerializationUtils.anyToJSON(WalletConfig("wallet-${Random().nextInt().absoluteValue}"))
    val walletCredentials = """{"key": "123"}"""

    val pool = PoolManager.openIndyPool(File(GENESIS_PATH), "pool-${Random().nextInt().absoluteValue}")

    try {
        Wallet.createWallet(walletConfig, walletCredentials).get()
    } catch (e: WalletExistsException) {
        // ok
    }

    val wallet = Wallet.openWallet(walletConfig, walletCredentials).get()

    return Pair(wallet, pool)
}

fun provideIndyUser(walletAndPool: Pair<Wallet, Pool>) = IndyUser(walletAndPool.second, walletAndPool.first, null, """{"seed": "000000000000000000000000Trustee1"}""")

fun provideApiClient(gson: Gson): SovrinAgentService {

    val retrofit: Retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl("http://10.255.255.21:8082")
            .build()

    return retrofit.create(SovrinAgentService::class.java)
}

fun provideGson(): Gson {
    return GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes): Boolean {
            return f.declaringClass == RealmObject::class.java
        }

        override fun shouldSkipClass(clazz: Class<*>): Boolean {
            return false
        }
    }).create()
}
