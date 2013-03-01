
#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "SmartObjects/CSmartObject.hpp"

namespace test { namespace components { namespace SmartObjects { namespace SmartObjectStressTest {

    using namespace NsAppLink::NsSmartObjects;

    class StressTestHelper : public ::testing::Test
    {
    private:
        std::vector<std::string>split(const std::string &s, char delim) const
        {
            std::vector<std::string> elems;

            std::stringstream ss(s);
            std::string item;
            while(std::getline(ss, item, delim))
            {
                elems.push_back(item);
            }

            return elems;
        }

        char get_random_char() const
        {
            return static_cast<char>('A' + (rand()%52));
        }

        std::string to_string(const int value) const
        {
            char buff[32];
            sprintf(buff, "%d", value);
            return std::string(buff);
        }

        std::string to_string(const double value) const
        {
            char buff[32];
            sprintf(buff, "%g", value);
            return std::string(buff);
        }

        std::string to_string(const char ch) const
        {
            char buff[2];
            sprintf(buff, "%c", ch);
            return std::string(buff);
        }

        std::string to_string(const bool b) const
        {
            return std::string( (b) ? "true" : "false");
        }

    protected:
        typedef std::map<std::string, std::string> VerificationMap;
        VerificationMap mVerifyMap;

        std::string generate_key(const char *pPrefix, const int index) const
        {
            char buff[32];
            sprintf(buff, "%s%d ", pPrefix, index);
            return std::string(buff);
        }

        void makeRandomObject(CSmartObject &obj, const int size, std::string key_path)
        {
            int type_id = rand() % 7;

            switch (type_id)
            {
            case 0:     // int
                {
                    int iVal = rand();
                    std::cout << iVal << std::endl;
                    obj = iVal;
                    mVerifyMap[key_path] = to_string(iVal);
                    break;
                }
            case 1:     // bool
                {
                    bool bVal = static_cast<bool>(rand()%2);
                    std::cout << bVal << std::endl;
                    obj = bVal;
                    mVerifyMap[key_path] = to_string(bVal);
                    break;
                }
            case 2:     // double
                {
                    double dVal = 100.0 / (rand()%200);
                    std::cout << dVal << std::endl;
                    obj = dVal;
                    mVerifyMap[key_path] = to_string(dVal);
                    break;
                }
            case 3:     // char
                {
                    char cVal = get_random_char();
                    std::cout << cVal << std::endl;
                    obj = cVal;
                    mVerifyMap[key_path] = to_string(cVal);
                    break;
                }
            case 4:     // string
                {
                    std::string strVal(rand()%200, get_random_char());
                    std::cout << strVal;
                    obj = strVal;   // string with random char filled random size
                    mVerifyMap[key_path] = strVal;
                    break;
                }
            case 5:     // map
                if (size <= 0)
                    break;

                std::cout << "map" << std::endl;

                mVerifyMap[key_path] = "map";
                for (int i = 0; i < 1; i++)
                {
                    std::string key = generate_key("M", i);
                    obj[key] = key;
                    //obj[key] = CSmartObject();
                    //makeRandomObject(obj[key], size - 1, key_path + key);     // recursion
                    std::cout << "created object: " << static_cast<std::string>(obj[key]) << " key: " << key_path + key << std::endl;
                }
                break;
            case 6:     // array
                if (size <= 0)
                    break;

                std::cout << "array" << std::endl;

                mVerifyMap[key_path] = "array";
                for (int i = 0; i < size; i++)
                {
                    obj[i] = CSmartObject();      // just init it as an array
                    makeRandomObject(obj[i], size - 1, key_path + generate_key("A", i));     // recursion
                }


                break;
            }
        }

        CSmartObject get_object(CSmartObject &rootObj, const std::string &path) const
        {
            std::vector<std::string> obj_tokens;
            CSmartObject lastObj = rootObj;

            obj_tokens = split(path, ' ');

            for (int i = 0; i < obj_tokens.size(); i++)
            {
                if (obj_tokens[i][0] == 'A')            // array
                {
                    int index = atoi(&(obj_tokens[i].c_str()[1]));      // get integer skipping first char
                    std::cout << " Array index=" << index;
                    //lastObj = lastObj[index];       // go to the child object
                    lastObj = CSmartObject(lastObj[index]);        // FIXME:  Workaround!!!
                }
                else if (obj_tokens[i][0] == 'M')       // map
                {
                    std::cout << " Map key=" << obj_tokens[i];
                    //lastObj = lastObj[obj_tokens[i]];       // go to the child object
                    lastObj = CSmartObject(lastObj[obj_tokens[i]]);     // FIXME: Workaround!!!
                }
                else
                {
                    //FAIL();
                    EXPECT_TRUE(false);
                }
            }
            return lastObj;
        }
    };

    /*
     * The test creates the initial CSmartObject and use it as an array for the next CSmartObjects.
     * Each next CSmartObject is randomly assigned to some type.
     * If one of the object happens to be a container it fills it with CSmartObject of random type. The amount of these
     * objects is the size of the parent container -1.
     * The iteration continues until all nodes are simple CSmartObjects (not arrays or maps)
     */
    TEST_F(StressTestHelper, StressTest)
    {
        CSmartObject objects;

        const int size = 16;

        for (int i = 0; i < size; i++)
        {
            CSmartObject obj;

            makeRandomObject(obj, size - 1, generate_key("A", i));

            objects[i] = obj;
        }


        for (VerificationMap::const_iterator it = mVerifyMap.begin(); it != mVerifyMap.end(); it++)
        {
            std::string value(it->second);

            if (value.compare("map") && value.compare("array"))
            {
                std::cout << "Key: " << it->first << " Val: " << value << std::endl;
                CSmartObject obj = get_object(objects, it->first);
                std::cout << " value: " << static_cast<std::string>(obj) << std::endl;

                ASSERT_EQ(value, static_cast<std::string>(obj)) << "Object value is not correct. Object path: " << it->first;
            }
        }
    }

    TEST_F(StressTestHelper, ExtraManualDebugTest)
    {
        CSmartObject obj;

        obj[0] = false;
        obj[1] = 0.869495;
        obj[2] = true;
        obj[3] = 'Q';
        obj[4] = true;
        obj[5] = 3.704;
        obj[6] = CSmartObject();
        obj[6][0] = std::string("ttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt");
        obj[6][1] = 'K';
        obj[6][2] = 0.735294;
        obj[6][3] = 'I';
        obj[6][4] = CSmartObject();
        obj[6][4]["M0"] = 0.59432;
        CSmartObject & refObj = obj[6][4];
        refObj["M1"]["M0"]["M0"][0] = true;

        ASSERT_EQ("0.59432", static_cast<std::string>(get_object(obj, "A6 A4 M0")));
        ASSERT_EQ("true", static_cast<std::string>(get_object(obj, "A6 A4 M1 M0 M0 A0")));
    }

    int main(int argc, char **argv)
    {
        ::testing::InitGoogleTest(&argc, argv);
        return RUN_ALL_TESTS();
    }

}}}}
