# lechiffre
Numbers everywhere.

# How the Statistics work

## Activity

Coming soon...

## Daily Activity

When you check the stats on a user (!stats <username>), you will get a block of stats. One of these is the "most daily activity" stat, which is
the hours that the user is most active on the server:

*most active hour in the day:  10 AM to 11 AM*

This is calculated in the following way:

The bot bins activity into 24 hours of the day. Each unique (to that day) activity since the !lastseen activity will increment the activity of the appropriate bin by 1.

### Example:

- A user types/comes online/changes status message at Nov 4, 10:45 -> the activity bin for 10am gets incremented by 1.
- They then proceed to write a book-worth of messages between 10:45 and 10:50 -> The bin for 10am remains unchanged, at 1.
- Then they go idle for 20mins, after-which they type a message at Nov 4, 11:10 -> The bin for 11am gets incremented by 1.
- They then bugger off for a day, and come back online Nov 5, 10:10 -> the bin for 10am gets incremented by 1.

If you executed a stats command on this user at this point, you would see most activity between 10am - 11am.
If there there are multiple top (most active) hours with the same value (i.e.  3am, 10am and 6pm all are 8), the first hour in the day (3am in this example) would be output as the "most active".