
def main():
    import commands, sys, codecs

    users = sys.argv[1]
    accGraph = sys.argv[2]
    curGraph = sys.argv[3]

    print "Saving current graph to %s..." % (accGraph)
    print 'Command:' + ' cat ' + curGraph + ' >> ' + accGraph
    commands.getoutput('cat ' + curGraph + ' >> ' + accGraph)

    print "Getting last processed user..."
    print 'Command:' + ' tail -1 ' + curGraph + ' | cut -f1 | cut -c4-'
    lastUser = commands.getoutput('tail -1 ' + curGraph + ' | cut -f1 | cut -c4-')
    print lastUser

    print 'Removing users that have been processed... from %s' % (users)
    fin = codecs.open(users, encoding = 'utf-8', mode = 'r')
    allUsers = fin.readlines()
    fin.close()
    print "Original Users: %d" % len(allUsers)

    index = 0
    for i in range(0, len(allUsers)):
        if allUsers[i].strip() == lastUser.strip():
            index = i + 1
            break

    del allUsers[0:index]

    fout = codecs.open(users, encoding = 'utf-8', mode = 'w')
    for user in allUsers:
        if not user.strip() == "":
            fout.write(user.strip() + "\n")
    fout.close()

    print "Remaining Users: %d" % len(allUsers)

main()
